module mymod {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.scripting;
    provides javax.script.ScriptEngineFactory with pseudoScriptEngine.RgfPseudoScriptEngineFactory;
    exports pseudoScriptEngine;
    exports myapp1;
}
