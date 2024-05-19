import pandas as pd
import numpy as np
from datetime import datetime
import plotly.express as px
import plotly.graph_objs as go
from textblob import TextBlob
from dash import Dash, dcc, html
from dash.dependencies import Input, Output
from wordcloud import WordCloud
import base64
from io import BytesIO
import nltk

# Télécharger les corpus nécessaires
nltk.download('punkt')
nltk.download('averaged_perceptron_tagger')

# Charger les données du CSV
df = pd.read_csv('student_data.csv')  # Remplacez par le chemin correct de votre fichier CSV

# Convertir les timestamps en datetime
df['Timestamp'] = pd.to_datetime(df['Timestamp'], unit='s')

# Extraire les informations nécessaires
df['Hour'] = df['Timestamp'].dt.hour
df['Month'] = df['Timestamp'].dt.month
df['Weekday'] = df['Timestamp'].dt.day_name()

# Analyse des sentiments
def analyze_sentiment(text):
    analysis = TextBlob(text)
    if analysis.sentiment.polarity > 0:
        return 'Positive'
    elif analysis.sentiment.polarity == 0:
        return 'Neutral'
    else:
        return 'Negative'

df['Sentiment'] = df['Text'].apply(analyze_sentiment)

# Créer les histogrammes initiaux
fig_hour = px.histogram(df, x='Hour', nbins=24, title='Nombre de lignes par heure de la journée')
fig_month = px.histogram(df, x='Month', nbins=12, title='Nombre de lignes par mois')
fig_sentiment = px.histogram(df, x='Sentiment', title='Distribution des sentiments')

# Série temporelle de l'analyse des sentiments par mois
sentiment_by_month = df.groupby(['Month', 'Sentiment']).size().reset_index(name='Count')
fig_sentiment_time_series = px.line(sentiment_by_month, x='Month', y='Count', color='Sentiment', title='Analyse de sentiments par mois')

# Nombre de fois où certains mots sont prononcés par jour de la semaine
keywords = ['Efrei', 'Pelouse', 'Profs']
df['Efrei'] = df['Text'].str.contains('Efrei', case=False)
df['Pelouse'] = df['Text'].str.contains('Pelouse', case=False)
df['Profs'] = df['Text'].str.contains('Profs', case=False)
keyword_counts = df.groupby(['Weekday'])[['Efrei', 'Pelouse', 'Profs']].sum().reset_index()

# Initialiser le graphique pour les mots-clés
def get_keyword_bar(day):
    filtered_data = keyword_counts[keyword_counts['Weekday'] == day]
    if filtered_data.empty:
        return go.Figure()  # Retourner un graphique vide si les données sont vides
    fig = go.Figure(data=[
        go.Bar(name='Efrei', x=filtered_data['Weekday'], y=filtered_data['Efrei']),
        go.Bar(name='Pelouse', x=filtered_data['Weekday'], y=filtered_data['Pelouse']),
        go.Bar(name='Profs', x=filtered_data['Weekday'], y=filtered_data['Profs']),
    ])
    fig.update_layout(barmode='group', title=f'Nombre de mentions par mots-clés pour {day}')
    return fig

# Nombre de phrases prononcées par campus
fig_campus = px.histogram(df, x='Campus', title='Nombre de phrases prononcées par campus')

# Répartition des étudiants par promo
fig_promo = px.histogram(df, x='Promo', title='Répartition des étudiants par promo')

# Carte de la répartition géographique des messages
fig_map = px.scatter_mapbox(df, lat="Latitude", lon="Longitude", hover_name="Text", hover_data=["Campus"],
                            color_discrete_sequence=["fuchsia"], zoom=12, height=300)
fig_map.update_layout(mapbox_style="open-street-map")
fig_map.update_layout(margin={"r":0,"t":50,"l":0,"b":0})
fig_map.update_layout(title="Répartition des phrases prononcées par lieux")

# Créer l'application Dash
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
            options=[{'label': day, 'value': day} for day in df['Weekday'].unique()],
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
])

@app.callback(
    Output('keyword-bar', 'figure'),
    [Input('day-dropdown', 'value')]
)
def update_keyword_bar(selected_day):
    return get_keyword_bar(selected_day)

if __name__ == '__main__':
    app.run_server(debug=True, port=8051)