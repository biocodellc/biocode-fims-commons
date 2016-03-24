package biocode.fims.utils;

import biocode.fims.fimsExceptions.FimsRuntimeException;

import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class Html2Text extends HTMLEditorKit.ParserCallback {
    StringBuffer s;

    public Html2Text() {

    }

    public String convert(String sp) {

        StringReader sr = new StringReader(sp);
        parse(sr);
        sr.close();
        return s.toString();
    }

    public void parse(Reader in) {
        s = new StringBuffer();
        ParserDelegator delegator = new ParserDelegator();
        // the third parameter is TRUE to ignore charset directive
        try {
            delegator.parse(in, this, Boolean.TRUE);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    @Override
    public void handleText(char[] text, int pos) {
        s.append(text);
    }

    @Override
    public void handleEndTag(HTML.Tag t,
                             int pos) {
        //s.append("\n");
    }

    public static void main(String[] args) {
            // the HTML to convert
            Html2Text parser = new Html2Text();

            System.out.println(parser.convert("<div>hello</div>"));
    }
}

