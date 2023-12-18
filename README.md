# README #

lrucache_client_cassandra - Cassandra client for VoltDB LRU Cache

### What is this repository for? ###

This repo contains the cassandra specific classes used by VoltDB's LRU cache implementation.

### How do I get set up? ###

For general information on how to use this see [lrucache_client](https://bitbucket.org/sr_mad_science/lrucache_client)

#### Configuration ####

You will need the latest VoltDB client library.

You will also need these Cassandra libraries or their up to date equivalents:

	cassandra-driver-core-3.3.0.jar
	cassandra-driver-extras-3.3.0.jar
	cassandra-driver-mapping-3.3.0.jar
	guava-23.0.jar
	log4j-over-slf4j-1.7.25.jar
	metrics-core-3.2.2.jar
	netty-all-4.1.15.Final.jar
	slf4j-api-1.7.25.jar

#### Dependencies ####

[voltseutil](https://bitbucket.org/voltdbseteam/voltseutil)

[voltseutil_cassandra](https://bitbucket.org/voltdbseteam/voltseutil_cassandra)

[lrucache_client](https://bitbucket.org/sr_mad_science/lrucache_client)


#### Database configuration ####

Both voltDB and the target database will have copies of the same table. So for example:

VoltDB: 

	CREATE TABLE subscriber  ( 
	  s_id    BIGINT NOT NULL PRIMARY KEY
	, sub_nbr VARCHAR(80) NOT NULL
	, f_tinyint tinyint
	, f_smallint smallint
	, f_integer integer
	, f_bigint bigint
	, f_float float
	, f_decimal decimal
	, f_geography geography
	, f_geography_point GEOGRAPHY_POINT
	, f_varchar varchar(80)
	, f_varbinary varbinary(1024)
	, flush_date TIMESTAMP 
	, last_use_date TIMESTAMP NOT NULL) ;

Cassandra:

	CREATE TABLE vtest.subscriber  (
	  s_id    BIGINT
	, sub_nbr text
	, f_tinyint tinyint
	, f_smallint smallint
	, f_integer int
	, f_bigint bigint
	, f_float float
	, f_decimal decimal
	, f_geography text
	, f_geography_point text
	, f_varchar varchar
	, f_varbinary blob
	, flush_date TIMESTAMP
	, last_use_date TIMESTAMP
	, primary key (s_id)) ;
	
### Usage guidelines ###

#### Connecting to Cassandra ####

In order to connect to Cassandra we need to instantiate CassandraRematerializerImpl and then call setConfig

    setConfig (String schemaName, String tableName, Properties config) 

	
The Rematerializer uses a properties object to contain additional parameters:

	database.hostname=192.168.0.50
	database.port=9042
	database.username=vtest
	database.tablename=SUBSCRIBER
	test.rematerializer=org.voltdb.lrucache.client.rematerialize.cassandra.CassandraRematerializerImpl

DATABASE_USERNAME and DATABASE_PASSWORD can be left out if needed. So an example usage would be:

	Properties config = new Properties();
			
	config.put(AbstractSqlRematerializer.DATABASE_HOSTNAME, "192.168.0.20");
	config.put(AbstractSqlRematerializer.DATABASE_PORT, "9042");
			
	theMissingDataCallback = new CassandraRematerializerImpl("vtest", "SUBSCRIBER", config,10) ;


### Technical Notes ###

#### Limitations ####

The current implementation is limited to around 1500 TPS.

### Contribution guidelines ###

Use  [lrucache_sdk](https://bitbucket.org/sr_mad_science/lrucache_sdk).

### Who do I talk to? ###

* drolfe@voltdb.com