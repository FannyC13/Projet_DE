### Context
This project was made for the course Data Engineering, taught by Adrien Broussolle for Efrei's Big Data and Machine Learning M1 class.
The aim of the project is to develop a service based on an IOT and an information system. The IOT devices must emit data in short lapses of time to emulate a the data flow of a successful startup.
The information system linked has to provide an alert service in addition to a long term analytics service.

### Storytelling
Paul Porte, an infamous alumni of the school suggested an innovative idea to the director : integrating tiny devices in every student's school card. Those devices will record the student's conversations as well as their geolocation, the exact time they enter the school, etc.
According to Paul Porte, the students aren't grateful enough for the school, hence the new module "Good Conduct". It's simple : every student start with 20 points, if but if they talk about a forbidden subject, they'll lose points. 
The IOT devices have to enter an alert state whenever a banword is pronounced, notify the student and decrease their grade.

![image](https://github.com/FannyC13/Projet_DE/assets/75856103/b33dfd95-648c-40ce-ac19-35a4bf0819fd)

In addition, the long-term analytics service will explore the trends going around the school to do some better marketing. Also, if a student talks often about basketball for example, then the staff will inform the basketball association that they might be able to hire a new member. 
Those extracts will be made every few weeks, depending on the needs of the school. It will also be able to track if people stay in the school after closing hours, which can be a security problem.

### Architecture

*Note : the architecture diagram is also available in addition to the readme.*

The producers will be the school cards, serving a purpose of IOT devices. They'll record the student's voices and format them using speech-to-text. 
The data will be transfered to a Kafka Topic : it will be redistributed twice, in Flink for the urgent scenario (banword detection) and in spark for the long term analysis.
On Flink's side : Flink will redistribute the data in machines able to calculate wether or not the whole sentence represented a violation of the school policy or not. A notification will be sent to the student and the staff, and the problematic data will go through a Cassandra database, flagged as problematic.
On Spark's side : Spark will directly pass the data through a Cassandra database. The data will be stored using hdfs on several machines. Through Cassandra's data, we'll be able to use Spark NLP or any other NLP service to analyse the conversations. Those data as well as the non-string data will then go onto analytics dashboards.

### Content of this repository 
- An architecture diagram
- A json serialiser/deserialiser
- The rest has yet to arrive
