package intercode.typechecker;

import intercode.parser.*;
import intercode.visitor.*;
import intercode.ast.*;
import intercode.lexer.*;


public class TypeChecker extends ASTVisitor{

    public Parser parser  = null;
    public CompilationUnit cu = null;
    
    public Env top = null;

    public int inLoop = 0;

    public Lexer lexer = null;

    int level = 0;
    String indent = "...";

    public TypeChecker (Parser parser){

        this.parser = parser;
        cu = this.parser.cu;
        visit(cu);
    }

    public TypeChecker(){
        visit(this.parser.cu);
    }
    
    void error (String s){
        println(s);
        exit(1);
    }

    void exit(int n){
        System.exit(n);
    }

    void print(String s){
        System.out.print(s);
    }

    void println(String s){
        System.out.println(s);
    }

    void printSpace(){
        System.out.print(" ");
    }

    int indent_level = 0;
    void indentUp(){
        indent_level ++;
    }

    void indentDown(){
        indent_level --;
    }

    void printIndent(){
        String s = "";
        for (int i = 0; i < indent_level; i++){
            s += "  ";
        }

        print(s);
    }

    void printDotDotDot(){

        String s = "";
        for (int i=0; i<indent_level; i++){
            s += indent;
        }
        print(s);
    }

    

    public void visit (CompilationUnit n) {
        System.out.println("*****************************");
        System.out.println("*     TypeChecker starts    *");
        System.out.println("*****************************");
        System.out.println();
        System.out.println("CompilationUnit");

        indentUp();
        n.block.accept(this) ;
        indentDown();
    }

    public void visit (BlockStatementNode n) {

        printDotDotDot();
        System.out.println("BlockStatementNode");

        n.sTable = top;
        top = new Env(top);


        indentUp();
        for (DeclarationNode decl : n.decls)
            decl.accept(this);
        indentDown();

        indentUp();
        for (StatementNode stmt : n.stmts){
            stmt.accept(this);
            //System.out.println(inLoop + "-------------------------StatementNode");
        }
            
        indentDown();

        top = n.sTable;

    }


    public void visit (DeclarationNode n) {

        printDotDotDot();
        System.out.println("DeclarationNode");

        indentUp();
        n.type.accept(this);
        indentDown();

        indentUp();
        n.id.accept(this);
        indentDown();

        top.put(n.id.w, n.id);

    }

    public void visit (TypeNode n) {

        printDotDotDot();
        System.out.println("TypeNode: " + n.basic);

        if (n.array != null){
            indentUp();
            n.array.accept(this);
            indentDown();
        }

    }

    public void visit (ArrayTypeNode n) {

        printDotDotDot();
        System.out.println("ArrayTypeNode: " + n.size);

        if (n.type != null){
            indentUp();
            n.type.accept(this);
            indentDown();
        }

    }

    public void visit (Statements n){

        if (n.stmts != null){

            n.stmt.accept(this);
            n.stmts.accept(this);
        }
    }

    public void visit(ParenthesesNode n){
        printDotDotDot();
        System.out.println("ParenthesesNode");

        indentUp();
        n.expr.accept(this);
        indentDown();
        n.type = n.expr.type;
    }

    public void visit(AssignmentNode n){

        printDotDotDot();
        System.out.println("AssignmentNode");

        // indentUp();
        // n.left.accept(this);
        // indentDown();
        
        // IdentifierNode leftId = (IdentifierNode)n.left;
        // Type leftType = leftId.type;

        n.left.accept(this);
         
        IdentifierNode leftId = new IdentifierNode();
        Type leftType;

        if(n.left instanceof ArrayAccessNode){
            leftId = ((ArrayAccessNode)n.left).id;
            leftType = leftId.type;
        }
        else{
            leftId = ((IdentifierNode)n.left);
            leftType = ((IdentifierNode)leftId).type;
        }

        leftId = (IdentifierNode)top.get(((IdentifierNode)leftId).w);


        if(leftId == null){
            error("Variable not declared: " + ((IdentifierNode)n.left).id);
        }

        indentUp();
        printDotDotDot();
        println("In TypeChecker, AssignmentNode's left type: " + leftType);
        indentDown();
        indentUp();

        Type rightType = null;
        IdentifierNode rightId = new IdentifierNode() ;

        if (n.right instanceof IdentifierNode){
            ((IdentifierNode)n.right).accept(this);
            
            rightId = ((IdentifierNode)n.right);

            rightId = (IdentifierNode)top.get(((IdentifierNode)rightId).w);
            
            if(rightId == null){
                error("Variable not declared: " + ((IdentifierNode)n.right).id);
            }

            rightType = rightId.type;

        }
        else if(n.right instanceof ArrayAccessNode){
            ((ArrayAccessNode)n.right).accept(this);
        }
        else if (n.right instanceof NumNode){
            
            ((NumNode)n.right).accept(this);
            rightType = Type.Int ;
        }
        else if (n.right instanceof RealNode){
            ((RealNode)n.right).accept(this);
            rightType = Type.Float;
        }
        else if (n.right instanceof ParenthesesNode){
            ((ParenthesesNode)n.right).accept(this);
        }
        else if (leftType != null && rightType != null){
                if (leftType != rightType)
                    error("[TypeError] Incompatible types in AssignmentNode: '" + leftType + "' and '" + rightType + "'");

        }
        else{
            println("VIsiting BinExprNode in AssignmentNode --------");
            
            ((BinExprNode)n.right).accept(this);
            rightType = ((BinExprNode)n.right).type ;
        }
        indentDown();

 //error("[TypeError] left-hand side type " + leftId.id + "=" + leftType + " of an assignment is incompatible to the right-hand side type");
            
    }
    
    public void visit (IdentifierNode n) {
        printDotDotDot();
        System.out.println("IdentifierNode: " + n.id);
    }

    public void visit (NumNode n) {
        printDotDotDot();
        System.out.println("NumNode: " + n.value);
    }
    
    public void visit (RealNode n) {
        printDotDotDot();
        System.out.println("RealNode: " + n.value);
    }

    public void visit (BinExprNode n) {
        
        printDotDotDot();
        System.out.println("BinExprNode: " + n.op);
        indentUp();

        Type leftType = null;
        IdentifierNode leftId = null;

        if (n.left instanceof IdentifierNode){
            ((IdentifierNode)n.left).accept(this);

            leftId = (IdentifierNode)top.get(((IdentifierNode)n.left).w);
            
            if(leftId == null){
                error("Variable not declared: " + ((IdentifierNode)n.left).id);
            }
            
            leftType = leftId.type;

        }
        else if(n.left instanceof NumNode){
            ((NumNode)n.left).accept(this);
            leftType = Type.Int;
        }
        else if (n.right instanceof RealNode){
            ((RealNode)n.right).accept(this);
            leftType = Type.Float;
        }
        else if(n.left instanceof ArrayAccessNode){
            ((ArrayAccessNode)n.left).accept(this);

            ArrayAccessNode tmp = (ArrayAccessNode)n.left;
            
            println("n.left Array in BinEpr ---------");
            leftId = (IdentifierNode)top.get(((IdentifierNode)tmp.id).w);
            
            if(leftId == null){
                error("Variable not declared: " + ((IdentifierNode)n.left).id);
            }

            leftType = leftId.type;
        }
        else{
            ((BinExprNode)n.left).accept(this);
        }

        Type rightType = null;
        IdentifierNode rightId = null;


        if (n.right != null){
            if (n.right instanceof IdentifierNode){
                ((IdentifierNode)n.right).accept(this);
                
                rightId = (IdentifierNode)top.get(((IdentifierNode)n.right).w);
            
                if(rightId == null){
                    error("Variable not declared: " + ((IdentifierNode)n.right).id);
                }
            
                rightType = rightId.type;

                if (leftType != rightType)
                    error("[TypeError] Incompatible types for operator '" + n.op + "' in BinexprNode: '" + leftType + "' and '" + rightType + "'");
        

            }
            else if(n.right instanceof NumNode){
                println("VIsiting NumNode in BinEprNode --------");
            
                ((NumNode)n.right).accept(this);
                rightType = Type.Int;

                if (leftType != rightType)
                    error("[TypeError] Incompatible types for operator '" + n.op + "' in BinexprNode: '" + leftType + "' and '" + rightType + "'");
        
            }
            else if (n.right instanceof RealNode){
                ((RealNode)n.right).accept(this);
                rightType = Type.Float;

                if (leftType != rightType)
                    error("[TypeError] Incompatible types for operator '" + n.op + "' in BinexprNode: '" + leftType + "' and '" + rightType + "'");
        
            }
            else if(n.right instanceof ArrayAccessNode){
                ((ArrayAccessNode)n.right).accept(this);

                ArrayAccessNode tmp = (ArrayAccessNode)n.right;

                rightId = (IdentifierNode)top.get(((IdentifierNode)n.right).w);
                
                if(rightId == null){
                    error("Variable not declared: " + ((IdentifierNode)n.right).id);
                }

                rightType = rightId.type;


                if (leftType != rightType)
                    error("[TypeError] Incompatible types for operator '" + n.op + "' in BinexprNode: '" + leftType + "' and '" + rightType + "'");
        
            }
            else{
                ((BinExprNode)n.right).accept(this);
            }
        }
        else{
            print("");
        }
        
        if (leftType != rightType)
            error("[TypeError] Incompatible types for operator '" + n.op + "' in BinexprNode: '" + leftType + "' and '" + rightType + "'");
        println("End of BinExprNode -----------");

        indentDown();
    }

    public void visit (WhileStatementNode n) {
        printDotDotDot();
        System.out.println("WhileStatementNode  -----------");
        inLoop++;
        System.out.println(inLoop);

        indentUp();
        n.cond.accept(this);
        indentDown();

        indentUp();
        n.stmt.accept(this);
        indentDown();
        if(inLoop>0){
            inLoop--;
        }
    }

    public void visit (IfStatementNode n) {
        printDotDotDot();
        System.out.println("IfStatementNode");

        indentUp();
        n.cond.accept(this);
        indentDown();

        indentUp();
        n.stmt.accept(this);
        indentDown();
        if (n.else_stmt != null){
            printDotDotDot();
            System.out.println("ElseStatementNode");

            indentUp();
            n.else_stmt.accept(this);
            indentDown();
        }
    }
    public void visit (DoWhileStatementNode n) {
        printDotDotDot();
        inLoop++;
        System.out.println("DoWhileStatementNode  --------" + inLoop);
        

        indentUp();
        n.stmt.accept(this);
        indentDown();
        
        if(!(n.stmt instanceof BreakStatementNode)){
            if(inLoop > 0){
                inLoop--;
            } else {
                error("[ERROR] BreakNode is not in loop");
            }
        }


        

        indentUp();
        n.cond.accept(this);
        indentDown();
    }
    public void visit(ArrayAccessNode n){
        printDotDotDot();
        println("ArrayAccessNode");

        indentUp();
        n.id.accept(this);
        indentDown();
       

        indentUp();

        
        // if(n.index.type != Type.Int){
        //     error("Array index not of Type Int near line " + n.lineNum);
        // }
        // else{
        //     n.index.accept(this);
        // }

        n.index.accept(this);
        indentDown();
    }
    public void visit(ArrayDimsNode n){
        printDotDotDot();
        println("ArrayDimsNode");

        IdentifierNode dimsId = null;
        Type dimsType = null;

        if (n.size instanceof IdentifierNode){

            ((IdentifierNode)n.size).accept(this);

            dimsId = ((IdentifierNode)n.size);
            dimsId = (IdentifierNode)top.get(((IdentifierNode)dimsId).w);

            if(dimsId == null){
                error("Variable not declared: " + ((IdentifierNode)n.size).id);
            }

            dimsType = dimsId.type ;

            if (dimsType != Type.Int)
                error("[Error] Array dimension is not of type int");
        }
        else if(n.size instanceof NumNode){
            ((NumNode)n.size).accept(this);
            NumNode dimsNum = (NumNode)n.size;
            dimsType = Type.Int ;
        }

        //System.out.println(n.size.type + "   ====????????????");
        indentUp();
        n.size.accept(this);
        indentDown();


        if(n.dim != null){
            indentUp();
            n.dim.accept(this);
            indentDown();
        }
        
    }
    public void visit (BreakStatementNode n) {
        printDotDotDot();
        
        System.out.println("BreakNode " + inLoop + " - inLoop counter");

        if(inLoop > 0){
            inLoop--;
        } else {
            error("[ERROR] BreakNode is not in loop");
        }
    }

    public void visit (TrueNode n) {
        printDotDotDot();
        System.out.println("TrueNode");
    }
    public void visit (FalseNode n) {
        printDotDotDot();
        System.out.println("FalseNode");
    }

}