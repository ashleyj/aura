/*
* Merged from https://github.com/MobiDevelop/robovm
*/
package aura.compiler.llvm;

public class PlainTextInstruction extends Instruction {
    private final String plainText;
    
    public PlainTextInstruction(String plainText) {
        this.plainText = plainText;
    }

    public String getPlainText() {
        return plainText;
    }
    
    @Override
    public String toString() {
        return plainText;
    }
}
