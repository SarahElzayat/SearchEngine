# Search-Engine
Google-like simple search engine, supporting phrase search. 

The main file to run is \src\main\java\com\example\demo\homeController.java

Note: The required Jars are attached in file Jars just add them to your project
Note: use Mongodb compass

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Crawler:
In Crawalr.java the crawler is provided that provides 5000 unique webpages.
In case of Disconnection During Crawlring when resuming the crawler continues from the last point he was in, not from the beginning(seeds)

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Indexer:
The links resulting from the crawler are indexed and saved in an inverted file so that to use during searching.

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Phrase Searching is supported by placing the phrase in "".

-------------------------------------------------------------------------------------------------------------------------------------------------------------
The results of the search are ranked according to relevance and popularity.

-------------------------------------------------------------------------------------------------------------------------------------------------------------
Snippets are added under each link.
