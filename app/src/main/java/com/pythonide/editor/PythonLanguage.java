package com.pythonide.editor;

import io.github.rosemoe.sora.lang.Language;
import io.github.rosemoe.sora.lang.analysis.Analyzer;
import io.github.rosemoe.sora.lang.analysis.StyleReceiver;
import io.github.rosemoe.sora.lang.styling.Span;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.TextReference;
import io.github.rosemoe.sora.widget.CodeEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonLanguage implements Language {
    
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "\\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b"
    );
    
    private static final Pattern STRING_PATTERN = Pattern.compile(
            "(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"]*\"|'[^']*')"
    );
    
    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "#.*$", Pattern.MULTILINE
    );
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b\\d+(\\.\\d+)?\\b"
    );
    
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "\\bdef\\s+(\\w+)\\s*\\("
    );

    private CodeEditor editor;
    
    public PythonLanguage(CodeEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public CodeEditor getEditor() {
        return editor;
    }
    
    @Override
    public void setEditor(CodeEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public Analyzer createAnalyzer(Content content, TextReference textRef, StyleReceiver receiver) {
        return new PythonAnalyzer(content, receiver);
    }
    
    @Override
    public int getInterruptionLevel() {
        return INTERRUPTION_LEVEL_STRONG;
    }
    
    @Override
    public void require(String name) {
    }
    
    @Override
    public void destroy() {
    }
    
    @Override
    public boolean isAutoCompleteChar(char ch) {
        return Character.isLetter(ch) || ch == '.' || ch == '_';
    }
    
    @Override
    public int getIndentAdvance(String text) {
        if (text.trim().endsWith(":")) {
            return 4;
        }
        return 0;
    }
    
    @Override
    public boolean useTab() {
        return false;
    }
    
    @Override
    public CharSequence format(CharSequence text) {
        return text;
    }
    
    @Override
    public Symbol getSymbolAt(int line, int column) {
        return null;
    }
    
    private class PythonAnalyzer implements Analyzer {
        
        private final Content content;
        private final StyleReceiver receiver;
        private volatile boolean running = true;
        
        PythonAnalyzer(Content content, StyleReceiver receiver) {
            this.content = content;
            this.receiver = receiver;
        }
        
        @Override
        public void reset(Content content, TextReference textRef) {
        }
        
        @Override
        public void insert(CharSequence chars, int line, int column) {
            analyze();
        }
        
        @Override
        public void delete(int startLine, int startColumn, int endLine, int endColumn) {
            analyze();
        }
        
        @Override
        public void analyze() {
            if (!running) return;
            
            new Thread(() -> {
                String text = content.toString();
                List<Span> spans = new ArrayList<>();
                
                // Highlight keywords (Orange)
                Matcher keywordMatcher = KEYWORD_PATTERN.matcher(text);
                while (keywordMatcher.find()) {
                    spans.add(new Span(keywordMatcher.start(), keywordMatcher.end(), 
                            TextStyle.makeStyle(TextStyle.BOLD, 0xffcc7832, 0)));
                }
                
                // Highlight strings (Green)
                Matcher stringMatcher = STRING_PATTERN.matcher(text);
                while (stringMatcher.find()) {
                    spans.add(new Span(stringMatcher.start(), stringMatcher.end(),
                            TextStyle.makeStyle(TextStyle.NORMAL, 0xff6a8759, 0)));
                }
                
                // Highlight comments (Gray Italic)
                Matcher commentMatcher = COMMENT_PATTERN.matcher(text);
                while (commentMatcher.find()) {
                    spans.add(new Span(commentMatcher.start(), commentMatcher.end(),
                            TextStyle.makeStyle(TextStyle.ITALIC, 0xff808080, 0)));
                }
                
                // Highlight numbers (Blue)
                Matcher numberMatcher = NUMBER_PATTERN.matcher(text);
                while (numberMatcher.find()) {
                    spans.add(new Span(numberMatcher.start(), numberMatcher.end(),
                            TextStyle.makeStyle(TextStyle.NORMAL, 0xff6897bb, 0)));
                }
                
                // Highlight function names (Yellow)
                Matcher functionMatcher = FUNCTION_PATTERN.matcher(text);
                while (functionMatcher.find()) {
                    spans.add(new Span(functionMatcher.start(1), functionMatcher.end(1),
                            TextStyle.makeStyle(TextStyle.NORMAL, 0xffffc66d, 0)));
                }
                
                receiver.setStyles(this, spans);
                
            }).start();
        }
        
        @Override
        public void stop() {
            running = false;
        }
        
        @Override
        public boolean isRunning() {
            return running;
        }
    }
}
