# batch processing with Akka

The purpose of this project is the implementation of a batch processing using [Akka](http://akka.io). It's more a
proof of concept than a full productive solution, there is no special error handling.

The program reads a file with comm separated values (csv), the lines are converted to records and they are then
processed. The time needed is recorded. There also exists a non-parallel version to compare the processing speed.

*This are the basic requirements*

- the sequence of data in the output must be the same as in the input even if there is no sequential number in the
input data
- it must be possible to process files with an arbitrary number of records without memory or performance problems

*for this test the following processing is done*

 - creation of a record which contains the original csv value
 - creation a copy of the record
 - calculate a fibonacci number or do a thread sleep
 - when writing the output, the original csv value is used so that input and output can be compared using the diff
 command. This is to ensure that the records are written in the correct order.

# the used actors

The following sections describe the used actors and the messages that are passed between them. The program uses a
pull-pattern which ensures that the the work to be done is distributed among the actors which are not busy at the
moment. The idea for this I took from this [blog entry](http://www.michaelpollmeier.com/akka-work-pulling-pattern/)

![Aktoren-Grafik](https://bitbucket.org/sothawo/akkabatch/downloads/AkkaBatch.svg)

## Reader

The Reader is the main actor of the whole program. The CSV2Record actors register with the Reader and the Reader gets
 the message to process a file. It reads the lines from the input file and takes care that there is always a limited
 number of records in the system. This prevents that the system is flooded with data and that the Writer has to
 buffer too many processed records if one records takes a little longer during processing.

### incoming messages

- Register is sent by a CSV2Record to register with the Reader. Thes message must be resent regularly because the
Reader removes CSV2Record actors which don't reregister after a certain time. This is to prevent that the Reader
sends data to actors which are no longer available.
- InitReader contains information about the file to process and is sent in the beginning by the Inbox of the system.
- GetWork is sent by a CSV2Record when this actor can process some data.
- RecordReceived, contains the record-id and is sent by the Writer if it has received the corresponding record. The
Reader then removes this record from the list of records that may have to be sent again.
- RecordsWritten is sent by the Writer when it has written some data to the output. The message contains the number
of written records and enables the Reader to read the next data.
- SendAgain is sent by the Reader itself. The purpose of this message is to check if some records need to be sent
again if the processing takes too long.

### outgoing messages

- WorkAvailable is sent to all registered CSV2Record actors when new data is available.
- DoWork, contains the record-id and the csv line, is sent to a CVS2Record actor in response to a GetWork message.
- WorkDone is sent to Inbox when all records are processed or there has been an error.

## Writer

The Writer is responsible to write the records it gets to the output file keeping the original order of the records.
To achieve this, the Writer has an internal buffer where the records are stored which can not yet be written because
they were processed faster than records which need to be written before them. The management of this records is done
by using the record id which is sequentially generated by the Reader.

### incming messages

- InitWriter, contains name and encoding of the output file. Sent by the Inbox object.
- ProcessRecord (contains the record-id, the processed record and the original csv line)

### outgoing messages

- InitReady is sent to the Inbox when the InitWriter message was received and the internal structures are
intialized; contains a flag signaling success.
- RecordReceived, contains a record-id and is sent to the Reader when a PrcoessRecord message was received to
keep the Reader from sending the record again into the system.
- RecordsWritten is sent to the Reader when output records have been written. The message contains the number of
written records.

## CSV2Record
This actor converts a line in csv format into a record. For simulation purposes the actor just uses some processing
time as well.

### incoming messages
- WorkAvailable, is sent from the Reader to the registered CSV2Record actors, when there is data to be processed.
- DoWork (contains the record-id and the csv-line), is sent from the Reader after the CSV2Record has sent a GetWork
message to the Reader.

### outgoing messages
- Register is sent regularly to the Reader.
- GetWork, is sent to the Reader when the actor is ready to do some work either after the previous work has been
processed or after the actor has received a WorkAvailable message.
- ProcessRecord (contains the record-iD, die the original line and the record),
is sent to a RecordModifier.

## RecordModifier
This actor does the processing of the record.

### incoming messages
- ProcessRecord (see CSV2Record)

### outgoing messages
- ProcessRecord (contains the processed record), is sent to the Writer.

