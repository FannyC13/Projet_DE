import pandas as pd
from datetime import datetime
import plotly.express as px
import plotly.graph_objs as go
from dash import Dash, dcc, html
from dash.dependencies import Input, Output
from pyspark.sql import SparkSession
from pyspark.sql.functions import col
import sparknlp
from sparknlp.base import DocumentAssembler, Finisher
from sparknlp.annotator import ViveknSentimentModel, Tokenizer, Normalizer
from pyspark.ml import Pipeline


spark = SparkSession.builder \
    .appName("IOT Dashboard") \
    .config("spark.jars.packages", "com.johnsnowlabs.nlp:spark-nlp_2.12:4.0.0") \
    .getOrCreate()
sparknlp.start()


def load_data_from_hdfs():
    try:
        print("Reading data from HDFS...")
        df_spark = spark.read.parquet("hdfs://localhost:8020/user/hdfs/processed_reports")
        df_spark.show()
        print("Data read from HDFS successfully.")
        
        
        df_spark = df_spark.withColumn("Timestamp", col("Timestamp").cast("timestamp"))
        print("Timestamp column type ensured.")
        
        return df_spark
    except Exception as e:
        print(f"Error loading data from HDFS: {e}")
        return spark.createDataFrame([], schema=None)  


def analyze_sentiment(df_spark):
    document_assembler = DocumentAssembler() \
        .setInputCol("Sentence") \
        .setOutputCol("document")
    
    tokenizer = Tokenizer() \
        .setInputCols(["document"]) \
        .setOutputCol("token")
    
    normalizer = Normalizer() \
        .setInputCols(["token"]) \
        .setOutputCol("normalized")
    
    sentiment_model = ViveknSentimentModel.pretrained() \
        .setInputCols(["document", "token"]) \
        .setOutputCol("sentiment")
    
    finisher = Finisher() \
        .setInputCols(["sentiment"]) \
        .setOutputCols(["Sentiment"]) \
        .setOutputAsArray(False)
    
    pipeline = Pipeline(stages=[
        document_assembler,
        tokenizer,
        normalizer,
        sentiment_model,
        finisher
    ])
    
    model = pipeline.fit(df_spark)
    result = model.transform(df_spark)
    
    return result


def preprocess_data(df):
    if df.isEmpty():
        return pd.DataFrame()

    
    pandas_df = pd.DataFrame(df.collect(), columns=df.columns)
    print("Spark DataFrame converted to Pandas DataFrame.")
    
    pandas_df['Hour'] = pandas_df['Timestamp'].dt.hour
    pandas_df['Month'] = pandas_df['Timestamp'].dt.month
    pandas_df['Weekday'] = pandas_df['Timestamp'].dt.day_name()

    return pandas_df


def create_plots(df):
    if df.empty:
        return {}, {}, {}, {}, {}, {}, {}

    fig_hour = px.histogram(df, x='Hour', nbins=24, title='Nombre de lignes par heure de la journée')
    fig_month = px.histogram(df, x='Month', nbins=12, title='Nombre de lignes par mois')
    fig_sentiment = px.histogram(df, x='Sentiment', title='Distribution des sentiments')

    sentiment_by_month = df.groupby(['Month', 'Sentiment']).size().reset_index(name='Count')
    fig_sentiment_time_series = px.line(sentiment_by_month, x='Month', y='Count', color='Sentiment', title='Analyse de sentiments par mois')

    keywords = ['Efrei', 'Pelouse', 'Profs']
    df['Efrei'] = df['Sentence'].str.contains('Efrei', case=False)
    df['Pelouse'] = df['Sentence'].str.contains('Pelouse', case=False)
    df['Profs'] = df['Sentence'].str.contains('Profs', case=False)
    keyword_counts = df.groupby(['Weekday'])[['Efrei', 'Pelouse', 'Profs']].sum().reset_index()

    def get_keyword_bar(day):
        filtered_data = keyword_counts[keyword_counts['Weekday'] == day]
        if filtered_data.empty:
            return go.Figure()  
        fig = go.Figure(data=[
            go.Bar(name='Efrei', x=filtered_data['Weekday'], y=filtered_data['Efrei']),
            go.Bar(name='Pelouse', x=filtered_data['Weekday'], y=filtered_data['Pelouse']),
            go.Bar(name='Profs', x=filtered_data['Weekday'], y=filtered_data['Profs']),
        ])
        fig.update_layout(barmode='group', title=f'Nombre de mentions par mots-clés pour {day}')
        return fig

    fig_campus = px.histogram(df, x='campus', title='Nombre de phrases prononcées par campus')
    fig_promo = px.histogram(df, x='promo', title='Répartition des étudiants par promo')

    fig_map = px.scatter_mapbox(df, lat="Latitude", lon="Longitude", hover_name="Sentence", hover_data=["campus"],
                                color_discrete_sequence=["fuchsia"], zoom=12, height=300)
    fig_map.update_layout(mapbox_style="open-street-map")
    fig_map.update_layout(margin={"r":0,"t":50,"l":0,"b":0})
    fig_map.update_layout(title="Répartition des phrases prononcées par lieux")

    return fig_hour, fig_month, fig_sentiment, fig_sentiment_time_series, fig_campus, fig_promo, fig_map


df_spark = load_data_from_hdfs()
df_spark = analyze_sentiment(df_spark)
df = preprocess_data(df_spark)
print(df)

fig_hour, fig_month, fig_sentiment, fig_sentiment_time_series, fig_campus, fig_promo, fig_map = create_plots(df)


app = Dash(__name__)

app.layout = html.Div(children=[
    html.H1(children='Dashboard des messages IOT', style={'font-family': 'Montserrat, sans-serif'}),

    dcc.Graph(
        id='hour-histogram',
        figure=fig_hour
    ),

    dcc.Graph(
        id='month-histogram',
        figure=fig_month
    ),
    
    dcc.Graph(
        id='sentiment-histogram',
        figure=fig_sentiment
    ),

    dcc.Graph(
        id='sentiment-time-series',
        figure=fig_sentiment_time_series
    ),

    html.Div([
        html.Label('Choisissez un jour de la semaine:'),
        dcc.Dropdown(
            id='day-dropdown',
            options=[{'label': day, 'value': day} for day in df['Weekday'].unique()] if not df.empty else [],
            value='Monday'
        ),
        dcc.Graph(
            id='keyword-bar'
        )
    ]),

    dcc.Graph(
        id='campus-histogram',
        figure=fig_campus
    ),
    
    dcc.Graph(
        id='promo-histogram',
        figure=fig_promo
    ),

    dcc.Graph(
        id='map',
        figure=fig_map
    ),

    
    dcc.Interval(
        id='interval-component',
        interval=10*1000,  
        n_intervals=0
    )
])

@app.callback(
    Output('keyword-bar', 'figure'),
    [Input('day-dropdown', 'value')]
)
def update_keyword_bar(selected_day):
    return get_keyword_bar(selected_day)

@app.callback(
    [Output('hour-histogram', 'figure'),
     Output('month-histogram', 'figure'),
     Output('sentiment-histogram', 'figure'),
     Output('sentiment-time-series', 'figure'),
     Output('campus-histogram', 'figure'),
     Output('promo-histogram', 'figure'),
     Output('map', 'figure'),
     Output('day-dropdown', 'options')],
    [Input('interval-component', 'n_intervals')]
)
def update_data(n_intervals):
    df_spark = load_data_from_hdfs()
    df_spark = analyze_sentiment(df_spark)
    df = preprocess_data(df_spark)
    fig_hour, fig_month, fig_sentiment, fig_sentiment_time_series, fig_campus, fig_promo, fig_map = create_plots(df)
    day_options = [{'label': day, 'value': day} for day in df['Weekday'].unique()] if not df.empty else []
    return fig_hour, fig_month, fig_sentiment, fig_sentiment_time_series, fig_campus, fig_promo, fig_map, day_options

if __name__ == '__main__':
    app.run_server(debug=True, port=8051)
