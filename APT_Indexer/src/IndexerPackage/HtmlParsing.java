package IndexerPackage;

import java.io.File; ///file Data type

//reading file
import java.io.IOException;

//for fetching words
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


//Elements
import org.jsoup.select.Elements;

/*Wait
take input parameter file name (source code)to be parsed
* */
public class HtmlParsing {
    Document doc;
    HtmlParsing (String str_html) throws IOException
    {
       doc= Jsoup.parse(str_html,"UTF-8");

    }
    public Elements Parse_Tags(String tag)
    {
        // Document doc= Jsoup.parse(new File("D:\\2nd-term\\OS\\Project\\SearchEngine\\APT_Indexer\\src\\URL1.txt"),"UTF-8");
        Elements paragraphs = doc.select(tag);

        return paragraphs;
    }
}
