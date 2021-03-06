package ca.ubc.ece.salt.pangor.test.learning;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import ca.ubc.ece.salt.pangor.analysis.learning.LearningUtilities;
import ca.ubc.ece.salt.pangor.learning.apis.KeywordUse.KeywordContext;

public class TestLearningUtilitiesContext {

	public void runTest(AstNode token, KeywordContext expected) {
		KeywordContext context = LearningUtilities.getTokenContext(token);
		Assert.assertEquals("getTokenContext returned an incorrect value.", expected, context);
	}

	@Test
	public void testClassTokenContext() {

		Name name = new Name(0, "Bear");
		FunctionNode node = new FunctionNode(0, name);

		AstRoot root = new AstRoot();
		root.addChild(node);

		runTest(name, KeywordContext.CLASS_DECLARATION);

	}

	@Test
	public void testMethodNameTokenContext() {

		Name name = new Name(0, "getName");
		FunctionNode node = new FunctionNode(0, name);

		AstRoot root = new AstRoot();
		root.addChild(node);

		runTest(name, KeywordContext.METHOD_DECLARATION);

	}

	@Test
	public void testKeywordTokenContext() {

		Name right = new Name(0, "null");
		Name left = new Name(0, "a");

		Assignment assignment = new Assignment(left, right);

		AstRoot root = new AstRoot();
		root.addChild(assignment);

		runTest(right, KeywordContext.ASSIGNMENT_RHS);

	}

	@Test
	public void testPackageTokenContext() {

		StringLiteral pack = new StringLiteral();
		pack.setQuoteCharacter('"');
		pack.setValue("fs");

		FunctionCall call = new FunctionCall();
		call.setTarget(new Name(0, "require"));
		call.addArgument(pack);

		VariableInitializer initializer = new VariableInitializer();
		initializer.setTarget(new Name(0, "fs"));
		initializer.setInitializer(call);

		VariableDeclaration declaration = new VariableDeclaration();
		declaration.addVariable(initializer);

		ExpressionStatement statement = new ExpressionStatement(declaration);

		AstRoot root = new AstRoot();
		root.addChild(statement);

		runTest(pack, KeywordContext.REQUIRE);

	}

	@Test
	public void testTypeOfContext() {

		Name target = new Name(0, "user");
		UnaryExpression lhs = new UnaryExpression(Token.TYPEOF, 0, target);
		Name rhs = new Name(0, "undefined");

		InfixExpression condition = new InfixExpression(Token.SHEQ, lhs, rhs, 0);

		IfStatement ifs = new IfStatement();
		ifs.setCondition(condition);
		ifs.setThenPart(new EmptyStatement());

		AstRoot root = new AstRoot();
		root.addChild(ifs);

//		System.out.println(root.toSource());

		runTest(lhs, KeywordContext.CONDITION);

	}

	@Test
	public void testContextOfInfixContext() {

		Name target = new Name(0, "user");
		UnaryExpression lhs = new UnaryExpression(Token.TYPEOF, 0, target);
		Name rhs = new Name(0, "undefined");

		InfixExpression condition = new InfixExpression(Token.SHEQ, lhs, rhs, 0);

		IfStatement ifs = new IfStatement();
		ifs.setCondition(condition);
		ifs.setThenPart(new EmptyStatement());

		AstRoot root = new AstRoot();
		root.addChild(ifs);

//		System.out.println(root.toSource());

		runTest(condition, KeywordContext.CONDITION);

	}

	@Test
	public void testMethodCallTokenContext() {

		StringLiteral file = new StringLiteral();
		file.setQuoteCharacter('"');
		file.setValue("/etc/init.d/httpd");

		Name target = new Name(0, "existsSync");

		FunctionCall call = new FunctionCall();
		call.setTarget(target);
		call.addArgument(file);

		ExpressionStatement statement = new ExpressionStatement(call);

		AstRoot root = new AstRoot();
		root.addChild(statement);

		runTest(target, KeywordContext.METHOD_CALL);

	}

	@Test
	public void testFieldTokenContext() {

		Name target = new Name(0, "path");
		Name field = new Name(0, "delimiter");

		PropertyGet access = new PropertyGet(target, field);

		VariableInitializer initializer = new VariableInitializer();
		initializer.setTarget(new Name(0, "delim"));
		initializer.setInitializer(access);

		VariableDeclaration declaration = new VariableDeclaration();
		declaration.addVariable(initializer);

		ExpressionStatement statement = new ExpressionStatement(declaration);

		AstRoot root = new AstRoot();
		root.addChild(statement);

		runTest(field, KeywordContext.ASSIGNMENT_RHS);

	}

	@Test
	public void testConstantTokenContext() {

		Name target = new Name(0, "buffer");
		Name field = new Name(0, "INSPECT_MAX_BYTES");

		PropertyGet access = new PropertyGet(target, field);

		VariableInitializer initializer = new VariableInitializer();
		initializer.setTarget(new Name(0, "max"));
		initializer.setInitializer(access);

		VariableDeclaration declaration = new VariableDeclaration();
		declaration.addVariable(initializer);

		ExpressionStatement statement = new ExpressionStatement(declaration);

		AstRoot root = new AstRoot();
		root.addChild(statement);

		runTest(field, KeywordContext.ASSIGNMENT_RHS);

	}

	@Test
	public void testArgumentTokenContext() {

		StringLiteral file = new StringLiteral();
		file.setQuoteCharacter('"');
		file.setValue("/etc/init.d/httpd");

		Name target = new Name(0, "existsSync");

		FunctionCall call = new FunctionCall();
		call.setTarget(target);
		call.addArgument(file);

		ExpressionStatement statement = new ExpressionStatement(call);

		AstRoot root = new AstRoot();
		root.addChild(statement);

		runTest(file, KeywordContext.ARGUMENT);

	}

	@Test
	public void testParameterTokenContext() {

		StringLiteral file = new StringLiteral();
		file.setQuoteCharacter('"');
		file.setValue("/etc/init.d/httpd");

		NumberLiteral num = new NumberLiteral(5.0);

		Name var = new Name(0, "x");
		Name name = new Name(0, "open");

		FunctionNode function = new FunctionNode(0, name);
		function.addParam(file);
		function.addParam(num);
		function.addParam(var);
		function.setBody(new Block());

		AstRoot root = new AstRoot();
		root.addChild(function);

		runTest(file, KeywordContext.PARAMETER_DECLARATION);
		runTest(num, KeywordContext.PARAMETER_DECLARATION);
		runTest(var, KeywordContext.PARAMETER_DECLARATION);

	}

	@Test
	public void testExceptionTokenContext() {

		Name exception = new Name(0, "err");

		CatchClause catchClause = new CatchClause();
		catchClause.setBody(new Block());
		catchClause.setVarName(exception);

		TryStatement tryStatement = new TryStatement();
		tryStatement.setTryBlock(new Block());
		tryStatement.addCatchClause(catchClause);

		AstRoot root = new AstRoot();
		root.addChild(tryStatement);

		runTest(exception, KeywordContext.EXCEPTION_CATCH);

	}

	@Test
	public void testEventTokenContext() {

		/* Register the event listener. */

		StringLiteral registerEvent = new StringLiteral();
		registerEvent.setQuoteCharacter('\'');
		registerEvent.setValue("open");

		Name registerObject = new Name(0, "frontDoor");
		Name on = new Name(0, "on");
		Name registerAction = new Name(0, "ring");
		PropertyGet registerTarget = new PropertyGet();
		registerTarget.setTarget(registerObject);
		registerTarget.setProperty(on);

		FunctionCall registerCall = new FunctionCall();
		registerCall.setTarget(registerTarget);
		registerCall.addArgument(registerEvent);
		registerCall.addArgument(registerAction);

		ExpressionStatement registerStatement = new ExpressionStatement(registerCall);

		/* Remove the event listener. */

		StringLiteral removeListenerEvent = new StringLiteral();
		removeListenerEvent.setQuoteCharacter('\'');
		removeListenerEvent.setValue("open");

		StringLiteral removeEvent = new StringLiteral();
		removeEvent.setQuoteCharacter('\'');
		removeEvent.setValue("open");

		Name removeObject = new Name(0, "frontDoor");
		Name removeListener = new Name(0, "removeListener");
		Name removeAction = new Name(0, "ring");
		PropertyGet removeTarget = new PropertyGet();
		removeTarget.setTarget(removeObject);
		removeTarget.setProperty(removeListener);

		FunctionCall removeCall = new FunctionCall();
		removeCall.setTarget(removeTarget);
		removeCall.addArgument(removeListenerEvent);
		removeCall.addArgument(removeAction);

		ExpressionStatement removeStatement = new ExpressionStatement(removeCall);

		/* Remove all event listeners. */

		StringLiteral removeAllListenerEvent = new StringLiteral();
		removeAllListenerEvent.setQuoteCharacter('\'');
		removeAllListenerEvent.setValue("open");

		StringLiteral removeAllEvent = new StringLiteral();
		removeAllEvent.setQuoteCharacter('\'');
		removeAllEvent.setValue("open");

		Name removeAllObject = new Name(0, "frontDoor");
		Name removeAllListener = new Name(0, "removeAllListeners");
		PropertyGet removeAllTarget = new PropertyGet();
		removeAllTarget.setTarget(removeAllObject);
		removeAllTarget.setProperty(removeAllListener);

		FunctionCall removeAllCall = new FunctionCall();
		removeAllCall.setTarget(removeAllTarget);
		removeAllCall.addArgument(removeAllListenerEvent);

		ExpressionStatement removeAllStatement = new ExpressionStatement(removeAllCall);

		/* Add the call to the script. */

		AstRoot root = new AstRoot();
		root.addChild(registerStatement);
		root.addChild(removeStatement);
		root.addChild(removeAllStatement);

//		System.out.println(root.toSource());
		runTest(registerEvent, KeywordContext.EVENT_REGISTER);
		runTest(removeListenerEvent, KeywordContext.EVENT_REMOVE);
		runTest(removeAllListenerEvent, KeywordContext.EVENT_REMOVE);

	}

}