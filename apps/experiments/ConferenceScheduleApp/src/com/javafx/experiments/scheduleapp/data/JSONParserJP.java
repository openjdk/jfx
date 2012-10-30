/*
 * Copyright (c) 2008, 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.javafx.experiments.scheduleapp.data;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;

/**
 * A SAX/Pull style parser for JSON. It provides event call backs as it reads 
 * the JSON stream keeping only the minimal state needed to provide key/value 
 * pairs and a depth.
 * 
 * Designed to be very small and light weight and as fast as possible even 
 * without a JIT.
 */
public class JSONParserJP {
    private static enum Type {STRING,NUMBER,BOOLEAN,NULL};
    
    /**
     * Parses the given url and prints out the callback events to aid debugging.
     * 
     * @param url The url to load and parse
     */
    public static void debugParse(String url) {
        try {
            parse(url, new PrintCallback(System.out));
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }
    }
    
    /**
     * Parses the given url and fires the data into the given callback.
     * 
     * @param url The url to load and parse
     * @param callback a generic callback to receive the parse events
     * @throws IOException if thrown by the stream
     */
    public static void parse(String url,Callback callback) throws IOException {
        InputStream in = null;
        try {
            final URL urlObj = new URL(url);
            in = urlObj.openStream();
            parse(new InputStreamReader(in,"UTF-8"), callback);
        } catch (IOException ex) {
            if (in != null) in.close();
            throw ex;
        }
    }
    
    /**
     * Parses the given url and fires the data into the given callback.
     * 
     * @param url The url to load and parse
     * @param callback a generic callback to receive the parse events
     * @throws IOException if thrown by the stream
     */
    public static void parsePost(String url,Callback callback, List<Pair<String,String>> properties) throws IOException {
        final StringBuilder urlParameters = new StringBuilder();
        boolean first = true;
        for (Pair<String,String> property: properties) {
            if (!first) {
                urlParameters.append('&');
            }
            urlParameters.append(property.getKey());
            urlParameters.append('=');
            urlParameters.append(URLEncoder.encode(property.getValue(), "UTF-8"));
            first = false;
        }
        System.out.println("urlParameters = " + urlParameters);

        final URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)urlObj.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.toString().getBytes().length));
        connection.setRequestProperty("Content-Language", "en-US");  
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        try (DataOutputStream out = new DataOutputStream (connection.getOutputStream ())) {
            out.writeBytes(urlParameters.toString());
            out.flush();
        } catch (IOException exception) {
            exception.printStackTrace();
            throw exception;
        }
        if (connection.getResponseCode() == 200) {
            try (InputStream in = connection.getInputStream()) {
                //Get Response
                parse(new InputStreamReader(in,"UTF-8"), callback);
            }
        } else {
            System.err.println("====== ERROR WITH HTTP POST TO ["+url+"] -- params="+urlParameters+" =========");
            System.err.println("====== RESPONSE "+connection.getResponseCode()+" ["+connection.getResponseMessage()+"] =========");
            try (InputStream in = connection.getErrorStream()) {
                //Get Response	
                BufferedReader rd = new BufferedReader(new InputStreamReader(in));
                String line;
                StringBuilder response = new StringBuilder(); 
                while((line = rd.readLine()) != null) {
                  response.append(line);
                  response.append('\r');
                }
                System.err.println("RESPONSE:\n"+response);
            }
        }
    }
   
    private static final int NORMAL           = 0;
    private static final int QUOTE            = 1;
    private static final int QUOTE_ESCAPE     = 2;
    private static final int QUOTE_UNICODE    = 3;
    private static final int NULL             = 4;
    private static final int TRUE             = 5;
    private static final int FALSE            = 6;
    
    /**
     * Parses the given input reader and fires the data into the given callback.
     *
     * @param reader the reader
     * @param callback a generic callback to receive the parse events
     * @throws IOException if thrown by the stream
     */
    public static void parse(Reader reader, Callback callback) throws IOException {
        Type valueType = null;
        List<String> blocks = new ArrayList<String>();
        String potentialObjectName = null;
        String currentObjectName;
        int depth = 0;
        int keywordCharCount = -1;
        char[] buffer = new char[1024 * 11];
        char[] charBuffer = new char[1024];
        int charBufferLength = 0;
        String currentToken;
        int state = NORMAL;
        int charCount = 0;
        int index = 0;
        String unicode = null;
        try {
            while (true) {
                if (charCount == index) {
                    // we have no data so read some more from the stream
                    charCount = reader.read(buffer);
                    index = 0;
                    // check if we reached the end of stream
                    if (charCount < 0) return;
                }
                char c = (char) buffer [index++];
                switch(state) {
                    case NORMAL:
                        switch (c) {
                            case 'n':
                                state = NULL;
                                keywordCharCount = 1;
                                continue;
                            case 't':
                                state = TRUE;
                                keywordCharCount = 1;
                                continue;
                            case 'f':
                                // this can either be the start of "false" or the end of a
                                // fraction numberValue...
                                if (charBufferLength > 0) {
                                    if(charBufferLength == charBuffer.length) {
                                        char[] newCharBuffer = new char[charBuffer.length*2];
                                        System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                                        charBuffer = newCharBuffer;
                                    }
                                    charBuffer[charBufferLength++] = 'f';
                                    continue;
                                }
//                                if (currentToken.length() > 0) {
//                                    currentToken.append('f');
//                                    continue;
//                                }
                                state = FALSE;
                                keywordCharCount = 1;
                                continue;
                            case '{':
                                currentObjectName = potentialObjectName;
                                blocks.add(currentObjectName);
                                callback.startObject(currentObjectName, depth);
                                depth ++;
                                potentialObjectName = null;
                                continue;
                            case '}':
                                // end any pending item
                                currentToken = new String(charBuffer,0,charBufferLength);
                                if (potentialObjectName != null) {
                                    // IS KEY VALUE
                                    callback.keyValue(potentialObjectName, currentToken, depth);
                                } else if(valueType != null) {
                                    switch(valueType) {
                                        case NULL:
                                            callback.stringValue(null, depth);
                                            break;
                                        case STRING:
                                            callback.stringValue(currentToken, depth);
                                            break;
                                        case BOOLEAN:
                                            callback.booleanValue(currentToken != null && currentToken.trim().equalsIgnoreCase("true"), depth);
                                            break;
                                        case NUMBER:
                                            try {
                                                callback.numberValue(Double.parseDouble(currentToken),depth);
                                            } catch (NumberFormatException err) { /* this isn't a numberValue! */ }
                                            break;
                                    }
                                }
                                // remove depth
                                depth --;
                                // send end object
                                String closingName = null;
                                if (blocks.size() > 0) {
                                    closingName = blocks.remove(blocks.size() - 1);
                                }
                                callback.endObject(closingName, depth);
                                // clean up
                                potentialObjectName = null;
                                currentObjectName = null;
                                valueType = null;
                                continue;
                            case '[':
                                currentObjectName = potentialObjectName;
                                blocks.add(currentObjectName);
                                callback.startArray(currentObjectName, depth);
                                depth ++;
                                potentialObjectName = null;
                                continue;
                            case ']':
                                // end any pending item
                                currentToken = new String(charBuffer,0,charBufferLength);
                                if (potentialObjectName != null) {
                                    // IS KEY VALUE
                                    callback.keyValue(potentialObjectName, currentToken, depth);
                                } else if(valueType != null) {
                                    switch(valueType) {
                                        case NULL:
                                            callback.stringValue(null, depth);
                                            break;
                                        case STRING:
                                            callback.stringValue(currentToken, depth);
                                            break;
                                        case BOOLEAN:
                                            callback.booleanValue(currentToken != null && currentToken.trim().equalsIgnoreCase("true"), depth);
                                            break;
                                        case NUMBER:
                                            try {
                                                callback.numberValue(Double.parseDouble(currentToken),depth);
                                            } catch (NumberFormatException err) { /* this isn't a numberValue! */ }
                                            break;
                                    }
                                }
                                // remove depth
                                depth --;
                                // send end array
                                closingName = null;
                                if (blocks.size() > 0) {
                                    closingName = blocks.remove(blocks.size() - 1);
                                }
                                callback.endArray(closingName, depth);
                                // cleanup
//                                currentToken.setLength(0);
                                charBufferLength = 0;
                                valueType = null;
                                continue;
                            case ' ':
                            case '\r':
                            case '\t':
                            case '\n':
                                // whitespace
                                continue;

                            case '"':
                                state = QUOTE;
                                continue;
                            case ':':
                                potentialObjectName = new String(charBuffer,0,charBufferLength);
                                charBufferLength = 0;
                                continue;
                            case ',':
                                currentToken = new String(charBuffer,0,charBufferLength);
                                if (potentialObjectName != null) {
                                    // IS KEY VALUE
                                    callback.keyValue(potentialObjectName, currentToken, depth);
                                } else if(valueType != null) {
                                    switch(valueType) {
                                        case NULL:
                                            callback.stringValue(null, depth);
                                            break;
                                        case STRING:
                                            callback.stringValue(currentToken, depth);
                                            break;
                                        case BOOLEAN:
                                            callback.booleanValue(currentToken != null && currentToken.trim().equalsIgnoreCase("true"), depth);
                                            break;
                                        case NUMBER:
                                            try {
                                                callback.numberValue(Double.parseDouble(currentToken),depth);
                                            } catch (NumberFormatException err) { /* this isn't a numberValue! */ }
                                            break;
                                    }
                                }
                                potentialObjectName = null;
                                charBufferLength = 0;
                                valueType = null;
                                continue;
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                            case '.':
                            case 'x':
                            case 'd':
                            case 'l':
                                valueType = Type.NUMBER;
                                if(charBufferLength == charBuffer.length) {
                                    char[] newCharBuffer = new char[charBuffer.length*2];
                                    System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                                    charBuffer = newCharBuffer;
                                }
                                charBuffer[charBufferLength++] = c;
                                continue;
                        }
                        break;
                    case QUOTE:
                        switch (c) {
                            case '"':
                                state = NORMAL;
                                valueType = Type.STRING;
                                continue;
                            case '\\': // TODO handle escaped quotes
                                unicode = "";
                                state = QUOTE_ESCAPE;
                                continue;
                        }
                        if(charBufferLength == charBuffer.length) {
                            char[] newCharBuffer = new char[charBuffer.length*2];
                            System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                            charBuffer = newCharBuffer;
                        }
                        charBuffer[charBufferLength++] = c;
                        break;
                    case QUOTE_ESCAPE:
                        switch (c) {
                            case 'u':
                                unicode = "";
                                state = QUOTE_UNICODE;
                                continue;
                            case 'n':
                                c = '\n';
                                break;
                            case 'r':
                                c = '\r';
                                break;
                            case 't':
                                c = '\t';
                                break;
                        }
                        if(charBufferLength == charBuffer.length) {
                            char[] newCharBuffer = new char[charBuffer.length*2];
                            System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                            charBuffer = newCharBuffer;
                        }
                        charBuffer[charBufferLength++] = c;
                        state = QUOTE;
                        continue;
                    case QUOTE_UNICODE:
                        unicode += c;
                        if (unicode.length() == 4) {
                            c = (char) Integer.parseInt(unicode, 16);
                            if(charBufferLength == charBuffer.length) {
                                char[] newCharBuffer = new char[charBuffer.length*2];
                                System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                                charBuffer = newCharBuffer;
                            }
                            charBuffer[charBufferLength++] = c;
                            state = QUOTE;
                        }
                        continue;
                    case NULL:
                        switch (keywordCharCount) {
                            case 1:
                                if (c != 'u') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'u' of \"null\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 2:
                                if (c != 'l') throw new IOException("JSON Parse exception: found '"+c+"' when expected 1st 'l' of \"null\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 3:
                                if (c != 'l') throw new IOException("JSON Parse exception: found '"+c+"' when expected 2nd 'l' of \"null\" at char["+index+"] of buffer +\n"+new String(buffer));
                                valueType = Type.NULL;
                                state = NORMAL;
                                continue;
                        }
                        throw new IOException("JSON Parse exception: in unexpected state at char["+index+"] of buffer +\n"+new String(buffer));
                    case TRUE:
                        switch (keywordCharCount) {
                            case 1:
                                if (c != 'r') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'r' of \"true\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 2:
                                if (c != 'u') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'u' of \"true\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 3:
                                if (c != 'e') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'e' of \"true\" at char["+index+"] of buffer +\n"+new String(buffer));
                                if(charBufferLength+4 >= charBuffer.length) {
                                    char[] newCharBuffer = new char[charBuffer.length*2];
                                    System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                                    charBuffer = newCharBuffer;
                                }
                                charBuffer[charBufferLength++] = 't';
                                charBuffer[charBufferLength++] = 'r';
                                charBuffer[charBufferLength++] = 'u';
                                charBuffer[charBufferLength++] = 'e';
                                valueType = Type.BOOLEAN;
                                state = NORMAL;
                                continue;
                        }
                        throw new IOException("JSON Parse exception: in unexpected state at char["+index+"] of buffer +\n"+new String(buffer));
                    case FALSE:
                        switch (keywordCharCount) {
                            case 1:
                                if (c != 'a') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'a' of \"false\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 2:
                                if (c != 'l') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'l' of \"false\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 3:
                                if (c != 's') throw new IOException("JSON Parse exception: found '"+c+"' when expected 's' of \"false\" at char["+index+"] of buffer +\n"+new String(buffer));
                                keywordCharCount ++;
                                continue;
                            case 4:
                                if (c != 'e') throw new IOException("JSON Parse exception: found '"+c+"' when expected 'e' of \"false\" at char["+index+"] of buffer +\n"+new String(buffer));
                                if(charBufferLength+5 >= charBuffer.length) {
                                    char[] newCharBuffer = new char[charBuffer.length*2];
                                    System.arraycopy(charBuffer, 0, newCharBuffer, 0, charBuffer.length);
                                    charBuffer = newCharBuffer;
                                }
                                charBuffer[charBufferLength++] = 'f';
                                charBuffer[charBufferLength++] = 'a';
                                charBuffer[charBufferLength++] = 'l';
                                charBuffer[charBufferLength++] = 's';
                                charBuffer[charBufferLength++] = 'e';
                                valueType = Type.BOOLEAN;
                                state = NORMAL;
                                continue;
                        }
                        throw new IOException("JSON Parse exception: in unexpected state at char["+index+"] of buffer +\n"+new String(buffer));
                }
            }
        } catch (IOException ex) {
            reader.close();
            throw ex;
        }
    }
    
    public static interface Callback {
        /**
        * Indicates that the parser ran into start of object '{'
        * 
        * @param objectName if this object is value of key/value pair the 
        *                   this is the key name, otherwise its null
        * @param depth      The current depth, number of parent objects and  
        *                   arrays that contain this object
        */
        public void startObject(String objectName, int depth);

        /**
        * Indicates that the parser ran into end of object '}'
        * 
        * @param objectName if this object is value of key/value pair the 
        *                   this is the key name, otherwise its null
        * @param depth      The current depth, number of parent objects and  
        *                   arrays that contain this object
        */
        public void endObject(String objectName, int depth);

        /**
        * Indicates that the parser ran into start of array '['
        * 
        * @param arrayName  if this array is value of key/value pair the 
        *                   this is the key name, otherwise its null
        * @param depth      The current depth, number of parent objects and  
        *                   arrays that contain this array
        */
        public void startArray(String arrayName, int depth);

        /**
        * Indicates that the parser ran into start of array ']'
        * 
        * @param arrayName  if this array is value of key/value pair the 
        *                   this is the key name, otherwise its null
        * @param depth      The current depth, number of parent objects and  
        *                   arrays that contain this array
        */
        public void endArray(String arrayName, int depth);

        /**
        * Submits a string vale from the JSON data, a JSON null is passed 
        * as (value == null)
        */
        public void stringValue(String value, int depth);

        /**
        * Submits a numeric value from the JSON data
        */
        public void numberValue(double value, int depth);
        
        /**
        * Submits a boolean value from the JSON data
        */
        public void booleanValue(boolean value, int depth);

        /**
        * This method is called when a key/value pair is detected within the json.
        *
        * @param key the key
        * @param value a stringValue value
        */
        public void keyValue(String key, String value, int depth);

        /**
        * This method indicates if the parsing job is canceled
        * 
        * @return true if the parser should stop where it is
        */
        public boolean isCanceled();
    }
    
    public static class PrintCallback implements Callback {
        private String indent = "";
        private final PrintStream out;
        
        public PrintCallback(PrintStream out) {
            this.out = out;
        }

        @Override public void startObject(String blockName, int depth) {
            out.println(indent+"startObject("+blockName+","+depth+")");
            indent += "    ";
        }

        @Override public void endObject(String blockName, int depth) {
            indent = indent.substring(0, indent.length()-4);
            out.println(indent+"endObject("+blockName+","+depth+")");
        }

        @Override public void startArray(String arrayName, int depth) {
            out.println(indent+"startArray("+arrayName+","+depth+")");
            indent += "    ";
        }

        @Override public void endArray(String arrayName, int depth) {
            indent = indent.substring(0, indent.length()-4);
            out.println(indent+"endArray("+arrayName+","+depth+")");
        }

        @Override public void stringValue(String value, int depth) {
            out.println(indent+"stringValue("+value+","+depth+")");
        }

        @Override public void numberValue(double value, int depth) {
            out.println(indent+"numberValue["+value+","+depth+")");
        }
        
        @Override public void booleanValue(boolean value, int depth) {
            out.println(indent+"booleanValue["+value+","+depth+")");
        }

        @Override public void keyValue(String key, String value, int depth) {
            out.println(indent+"keyValue("+key+" => "+value+","+depth+")");
        }

        @Override public boolean isCanceled() { return false; }
    }
    
    /**
     * An adapter to make it cleaner in implementations that don't need to implement everything.
     */
    public static abstract class CallbackAdapter implements Callback {
        @Override public void startObject(String objectName, int depth) { }
        @Override public void endObject(String objectName, int depth) { }
        @Override public void startArray(String arrayName, int depth) { }
        @Override public void endArray(String arrayName, int depth) { }
        @Override public void stringValue(String value, int depth) { }
        @Override public void numberValue(double value, int depth) { }
        @Override public void booleanValue(boolean value, int depth) { }
        @Override public void keyValue(String key, String value, int depth) { }
        @Override public boolean isCanceled() { return false; }
    }
}
