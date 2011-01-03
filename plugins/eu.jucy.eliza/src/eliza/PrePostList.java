package eliza;

import java.util.ArrayList;


/**
 *  Eliza prePost list.
 *  This list of pre-post entries is used to perform word transformations
 *  prior to or after other processing.
 */
@SuppressWarnings("serial")
public class PrePostList extends ArrayList<PrePost> {

    /**
     *  Add another entry to the list.
     */
    public void add(String src, String dest) {
        add(new PrePost(src, dest));
    }

    /**
     *  Prnt the pre-post list.
     */
    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            PrePost p = (PrePost)get(i);
            p.print(indent);
        }
    }

    /**
     *  Translate a string.
     *  If str matches a src string on the list,
     *  return he corresponding dest.
     *  If no match, return the input.
     */
    String xlate(String str) {
        for (int i = 0; i < size(); i++) {
            PrePost p = get(i);
            if (str.equals(p.src())) {
                return p.dest();
            }
        }
        return str;
    }

    /**
     *  Translate a string s.
     *  (1) Trim spaces off.
     *  (2) Break s into words.
     *  (3) For each word, substitute matching src word with dest.
     */
    public String translate(String s) {
        String lines[] = new String[2];
        String work = EString.trim(s);
        s = "";
        while (EString.match(work, "* *", lines)) {
            s += xlate(lines[0]) + " ";
            work = EString.trim(lines[1]);
        }
        s += xlate(work);
        return s;
    }
}
