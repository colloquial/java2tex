package com.colloquial.java2tex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Java2Tex {

    static final Pattern SPACE_PATTERN = Pattern.compile("\\s*");

    static final String JAVA_FILE_REGEX = "\\.java$";
    static final String[] EMPTY_STRING_ARRAY = new String[0];


    static final String ASTERISK = "\\u002A";
    static final String FWD_SLASH = "\\u002F";
    static final String START_COMMENT = FWD_SLASH + ASTERISK;
    static final String END_COMMENT = ASTERISK + FWD_SLASH;
    static final String ALPHA_NUM = "\\w";
    static final String PERIOD = "\\u002E";
    static final String UNDERSCORE = "\\u005F";
    static final String HYPHEN = "\\u002D";

    static final String FILE_NAME_CHAR
        = "["
        + ALPHA_NUM
        + "" + PERIOD
        + "" + UNDERSCORE
        + "" + HYPHEN
        + "]";

    static final String START_REGEX
        = "(\\s*)" + START_COMMENT + "x (.+)" + END_COMMENT;

    static final String END_REGEX
        = START_COMMENT + "x" + END_COMMENT;

    static final Pattern START_PATTERN = Pattern.compile(START_REGEX);
    static final Pattern END_PATTERN = Pattern.compile(END_REGEX);
    static final int INITIAL_SPACES_GROUP_ID = 1;
    static final int FILE_NAME_GROUP_ID = 2;

    static final String TAB_REPLACEMENT = "    ";
    static final int TAB_LENGTH = TAB_REPLACEMENT.length();


    public static void main(String[] args) throws Exception {
        if (args.length < 5 || args.length > 6) {
            printUsage("Wrong number of arguments.",args);
        }
        File inPath = new File(args[0]);
        if (!inPath.exists()) {
            printUsage("Input path does not exist.",args);
        }
        File outDir = new File(args[1]);
        if (!outDir.exists() || !outDir.isDirectory()) {
            printUsage("Output directory does not exist.",args);
        }
        int lineLength = Integer.parseInt(args[2]);
        String inEncoding = args[3];
        String outEncoding = args[4];
        Pattern pattern = null;
        try {
            pattern
                = Pattern.compile(args.length == 6
                                  ? args[5]
                                  : JAVA_FILE_REGEX);
        } catch (PatternSyntaxException e) {
            printUsage("Illegal pattern exception=" + e, args);
        }

        System.out.println("Java 2 Tex");
        System.out.println("  IN PATH=" + inPath.getCanonicalPath());
        System.out.println("  OUT DIR=" + outDir.getCanonicalPath());
        System.out.println("  LINE LENGTH=" + lineLength);
        System.out.println("  INPUT (CODE) CHAR ENCODING=" + inEncoding);
        System.out.println("  OUTPUT (TEX) CHAR ENCODING=" + outEncoding);
        System.out.println("  FILE PATTERN=" + pattern);
        System.out.println("\n");

        walk(inPath,outDir,lineLength,inEncoding,outEncoding,pattern);
    }

    static void printUsage(String errorMsg, String[] args) {
        StringBuilder msg = new StringBuilder();
        msg.append("USAGE:\n");
        msg.append("\n");
        msg.append("Java2Tex inPath outDir lineLength encoding ?filePattern\n");
        msg.append("    inPath: Path on which to find files to process\n");
        msg.append("    outDir: Directory into which results are written\n");
        msg.append("    lineLength: Maximum number of characters per line before breaking\n");
        msg.append("    encoding: Code character encoding for input and output\n");
        msg.append("    filePattern: [optional] Java regex to find on file canonical names\n");
        msg.append("\n");

        msg.append("FOUND:\n");
        if (args.length == 0) {
            msg.append("no arguments given");
        } else {
            for (int i = 0; i < args.length; ++i)
                msg.append("     args[" + i + "]=" + args[i] + "\n");
        }
        msg.append("\n");

        msg.append("\nERROR:\n");
        msg.append(errorMsg);
        System.out.println(msg);
    }

    static void walk(File inPath, File outDir, int lineLength, String inEncoding, String outEncoding, Pattern pattern)
        throws IOException {

        if (inPath.isFile()) {
            if (pattern.matcher(inPath.getCanonicalPath()).find()) {
                // System.out.println("+" + inPath);
                process(inPath,outDir,lineLength,inEncoding,outEncoding);
            } else {
                // System.out.println("-" + inPath);
            }
        } else {
            for (File file : inPath.listFiles())
                walk(file,outDir,lineLength,inEncoding,outEncoding,pattern);
        }
    }


    static void process(File in, File outDir, int lineLength, String inEncoding, String outEncoding) 
        throws IOException {

        String[] inLines = readLines(in,inEncoding);
        for (int i = 0; i < inLines.length; ++i) {
            String line = inLines[i].replaceAll("\\t",TAB_REPLACEMENT);
            Matcher matcher = START_PATTERN.matcher(line);
            if (matcher.find()) {
                int outputLineCount = 0;
                String initialSpaces = matcher.group(INITIAL_SPACES_GROUP_ID);
                int numInitialSpaces = initialSpaces.length();
                String fileName = matcher.group(FILE_NAME_GROUP_ID);
                StringBuilder sb = new StringBuilder();
                for (++i; i < inLines.length; ++i) {
                    line = inLines[i].replaceAll("\\t",TAB_REPLACEMENT);
                    Matcher endMatcher = END_PATTERN.matcher(line);
                    if (endMatcher.find()) {
                        break;
                    } else {
                        int chopIndex
                            = Math.min(line.length(),
                                       numInitialSpaces);
                        String chopped = line.substring(0,chopIndex);
                        String restOfLine = line.substring(chopIndex);
                        if (!allSpaces(chopped)) {
                            System.out.println("\nWARNING: CHOPPED LINE"
                                               + "\n     file: " + in
                                               + "\n     line: " + (i+1)
                                               + "\n  chopped: " + chopped
                                               + "\n     rest:" + restOfLine
                                               + "\n");
                        }
                        addSpaces(sb,restOfLine,outputLineCount != 0,lineLength,i,in);
                        ++outputLineCount;
                        sb.append('\n');
                    }
                }
                fileName = trim_mac_junk(fileName);
                File outFile = new File(outDir,fileName);
                System.out.println("*|" + fileName + "|");
                String text = sb.toString();
                writeToFile(text,outFile,outEncoding);
            }
        }
    }

    static String trim_mac_junk(String filename) {
        return Character.isLetterOrDigit(filename.charAt(filename.length() - 1))
            ? filename
            : filename.substring(0,filename.length()-1);
    }

    static String[] splits(String line, int maxLineLength) {
        List<String> lineList = new ArrayList<String>();
        for (int pos = 0; pos < line.length(); pos = scan(lineList,line,pos,maxLineLength)) ;
        return lineList.toArray(EMPTY_STRING_ARRAY);
    }

    static int scan(List<String> lineList, String line, int pos, int maxLineLength) {
        int start = pos;
        StringBuilder sb = new StringBuilder();
        int renderedLength = 0;
        while (pos < line.length() && renderedLength < maxLineLength) {
            if (line.charAt(pos) == '/') {
                String subLine = line.substring(pos);
                if (subLine.startsWith("/*bbf*/")
                    || subLine.startsWith("/*ebf*/")) {
                    pos += "/*bbf*/".length();
                    continue;
                }
            }
            ++pos;
            ++renderedLength;
        }
        lineList.add(line.substring(start,pos));
        return pos;
    }

    static void addSpaces(StringBuilder sb, String line, boolean addNewLine, int maxLineLength,
                          int lineNumber, File inFile) {
        if (allSpaces(line)) {
            sb.append(addNewLine ? "\\empLine{}" : "%");
            return;
        }

        String[] splits = splits(line,maxLineLength);
        if (splits.length == 1) {
            addSpaces2(sb,line,addNewLine,false,false);
        } else {
            System.out.println("     split at line=" + (lineNumber+1) + " file=" + inFile);
            addSpaces2(sb,splits[0],addNewLine,true,false);
            for (int i = 1; i < splits.length - 1; ++i) {
                addSpaces2(sb,splits[i],true,true,true);
            }
            addSpaces2(sb,splits[splits.length-1],true,false,true);
        }
    }

    static void addSpaces2(StringBuilder sb, String line, 
                           boolean addNewLine,
                           boolean addContinuationEnd,
                           boolean addContinuationStart) {
        // escape tex syms: # $ % ^ & _ { } ~ \
        // as: \# \$ \% \^{} \& \_ \{ \} \~{}


        sb.append(tabs(line,addNewLine));
        
        if (addContinuationStart)
            sb.append("{\\brkbol}");
        
        String modLine
            = line
            // .trim()
            .replace("\\","\\bk")
            .replace("{","\\{")
            .replace("}","\\}")
            .replace("\\bk","{\\bk}")
            .replace("#","\\#")
            .replace("$","\\$")
            .replace("%","\\%")
            .replace("^","\\^{}")
            .replace("&","\\&")
            .replace("_","\\_")
            .replace("~","\\~{}")
            .replace("! ","!\\ ")
            .replace("? ","?\\ ")
            .replace(". ",".\\ ")
            .replace(": ",":\\ ")
            .replace("--","{-}{-}")
            .replace(">","{>}")
            .replaceAll(BBF_REGEX,"\\\\cdBold{")  // /*bbf*/ -> \cdBold{ 
            .replaceAll(EBF_REGEX,"}");           // /*ebf*/ -> }
        sb.append("\\mbox{" + modLine + "}");
        if (addContinuationEnd)
            sb.append("{\\brkeol}");
    }


    static String BBF_REGEX = "/(\\x2A)bbf(\\x2A)/";  // /*bbf*/

    static String EBF_REGEX = "/(\\x2A)ebf(\\x2A)/";  // /*ebf*/

    // static Pattern BF_PATTERN = Pattern.compile(BBF_REGEX + "|" + EBF_REGEX); // same length

    static String tabs(String line, boolean addNewLine) {
        int spaceCount = 0;
        for (int i = 0; i < line.length(); ++i) {
            if (line.charAt(i) == '\t')
                spaceCount += TAB_LENGTH;
            else if (line.charAt(i) == ' ')
                spaceCount += 1;
            else break;
        }
        return ( addNewLine ? "\\brkLine" : "\\plnLine")
            + "{" + spaceCount + "}";

    }

    static void writeToFile(String text, File file, String outEncoding) throws IOException {
        OutputStream out = null;
        Writer writer = null;
        try {
            out = new FileOutputStream(file);
            writer = new OutputStreamWriter(out,outEncoding);
            writer.write(text);
        } finally {
            try {
                if (writer != null) {
                    try { writer.close(); } catch (IOException e) { /* eat it */ }
                }
            } finally {
                if (out != null) {
                    try { out.close(); } catch (IOException e) { /* eat it */ }
                }
            }
        }
    }

    static boolean allSpaces(String x) {
        return SPACE_PATTERN.matcher(x).matches();
    }

    static String[] readLines(File file, String encoding)
        throws IOException {

        InputStream in = null;
        Reader reader = null;
        BufferedReader buf = null;
        try {
            in = new FileInputStream(file);
            reader = new InputStreamReader(in,encoding);
            buf = new BufferedReader(reader);
            List<String> lineList = new ArrayList<String>();
            String line;
            while ((line = buf.readLine()) != null)
                lineList.add(line);
            return lineList.toArray(EMPTY_STRING_ARRAY);
        } finally {
            if (buf != null) {
                try { buf.close(); } catch (IOException e) { /* eat it */ }
            }
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { /* eat it */ }
            }
            if (in != null) {
                try { in.close(); } catch (IOException e) { /* eat it */ }
            }
        }

    }

}