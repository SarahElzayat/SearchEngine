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

<!-- Contributors -->
## <img  align= center width=50px height=50px src="https://media1.giphy.com/media/WFZvB7VIXBgiz3oDXE/giphy.gif?cid=6c09b952tmewuarqtlyfot8t8i0kh6ov6vrypnwdrihlsshb&rid=giphy.gif&ct=s"> Contributors <a id = "contributors"></a>

<!-- Contributors list -->
<table align="center" >
  <tr>
    <td align="center"><a href="https://github.com/SarahElzayat"><img src="https://avatars.githubusercontent.com/u/76779284?v=4" width="150px;" alt=""/><br /><sub><b>Ahmed Hany</b></sub></a></td>
    <td align="center"><a href=""https://github.com/emanshahda" ><img src="https://avatars.githubusercontent.com/u/89708797?v=4" width="150px;" alt=""/><br /><sub><b>Eman Shahda</b></sub></a><br />
    <td align="center"><a href="https://github.com/zeinabmoawad"><img src="https://avatars.githubusercontent.com/u/92188433?v=4" width="150px" width="150px;" alt=""/><br /><sub><b>Zeinab Moawad</b></sub></a><br />
    <td align="center"><a href="https://github.com/BasmaElhoseny01"><img src="https://avatars.githubusercontent.com/u/72309546?v=4" width="150px;" alt=""/><br /><sub><b>Basma Elhoseny</b></sub></a><br /></td>
  </tr>
</table>

## <img  align= center width=50px height=50px src="https://media1.giphy.com/media/ggoKD4cFbqd4nyugH2/giphy.gif?cid=6c09b9527jpi8kfxsj6eswuvb7ay2p0rgv57b7wg0jkihhhv&rid=giphy.gif&ct=s"> License <a id = "license"></a>
This software is licensed under MIT License, See [License](https://github.com/SarahElzayat/SearchEngine/blob/main/LICENSE) for more information ©Sarah Elzayat.
