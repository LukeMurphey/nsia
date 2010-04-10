package net.lukemurphey.nsia.tests;

import java.io.UnsupportedEncodingException;
import junit.framework.TestCase;
import net.lukemurphey.nsia.scan.*;
import net.lukemurphey.nsia.scan.scriptenvironment.Variables;

public class PatternDefinitionTest extends TestCase {
	
	public void testParseBasic() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSignature = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}");
		
		if( threatSignature == null ){
			fail("The signature is null");
		}
		else if( !threatSignature.getCategoryName().equals("Test") ){
			fail("The signature name is not correct");
		}
		else if( !threatSignature.getSubCategoryName().equals("Exec") ){
			fail("The signature secondary name is not correct");
		}
	}
	
	public void testParseMultiple() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition[] threatSignatures = PatternDefinition.parseAll("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}\nAlert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}");
		
		if( threatSignatures.length != 2){
			fail("Not all signatures where parsed (returned " + threatSignatures.length + ")");
		}
	}
	
	public void testParseMultipleQuotedName() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition[] threatSignatures = PatternDefinition.parseAll("Alert(\"Test.Exec.Start\"){Severity=Low; Message=Exploit; String=L33t;}\nAlert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}");
		
		if( threatSignatures.length != 2){
			fail("Not all signatures where parsed (returned " + threatSignatures.length + ")");
		}
	}
	
	public void testParseMultipleMisQuotedName() throws UnpurposedDefinitionException{
		PatternDefinition[] threatSignatures;
		try {
			threatSignatures = PatternDefinition.parseAll("Alert(Test.Exec.Start\"){Message=Exploit; String=L33t;}\nAlert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}");
		} catch (InvalidDefinitionException e) {
			if( !e.getMessage().endsWith("(line 1)")){
				fail("Failed to identify the location of the invalid signature: " + e.getMessage());
			}
			return;
		}
		
		fail("Failed to identify invalid signature (returned " + threatSignatures.length + ")");
	}
	
	/*public void testParseMultipleLargeDataSet() throws IOException, InvalidSignatureException, UnpurposedRuleException{
		String rules = readFileAsString("/home/luke/NSIA/Development/Rules/Ethnic Asperions.rule");
		
		ThreatSignature[] threatSignatures = ThreatSignature.parseAll(rules);
		System.out.println( "Signatures parsed : " + threatSignatures.length );
		
	}*/
	
	public void testParseMultipleMisQuotedName2() throws UnpurposedDefinitionException{
		PatternDefinition[] threatSignatures;
		try {
			threatSignatures = PatternDefinition.parseAll("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t;}\nAlert(Test.Exec.Start\"){Severity=Low; Message=Exploit; String=L33t;}");
		} catch (InvalidDefinitionException e) {
			if( !e.getMessage().endsWith("line 2")){
				fail("Failed to identify the location of the invalid signature: " + e.getMessage());
			}
			return;
		}
		
		fail("Failed to identify invalid signature (returned " + threatSignatures.length + ")");
	}
	
	public void testParseToString() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSignature = PatternDefinition.parse("Alert(OffensiveLanguage.EthnicAspersion.Word27){Severity=Low; Message=\"Ethnic Aspersion (Cracker)\"; String=L33t;}");
		
		if( !threatSignature.toString().equals("OffensiveLanguage.EthnicAspersion.Word27") ){
			fail("The toString method did not return the expected value");
		}
			
	}
	
	public void testParseWithDoubleQuotes() throws InvalidDefinitionException, UnpurposedDefinitionException{		
		PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; Regex=\"/(((URL|SRC|HREF|LOWSRC)[\\s]*=)|(url[\\s]*[\\(]))[\\s]*['\\\"]*vbscript[\\:]/i\";}");
	}
	
	public void testParseWithAmbiguousSets() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=AVC; Set=Test1; UnSet=Test1; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect variable setting that is ambiguous");
	}
	
	public void testParseWithMisplacedQuotesInName() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(\"Test.Test.Te\"st\"){ Severity=Low; Message=\"Remote Code Execution\"; String=\"AVC\"; IfNotSet=Test1; IfSet=Test1; }");
		}
		catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect misplaced double quotes");
	}
	
	public void testParseWithEscapeCharsInName() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(\"Test.Test.Te\\cst\"){ Severity=Low; Message=\"Remote Code Execution\"; String=\"AVC\"; IfNotSet=Test1; IfSet=Test1; }");
		}
		catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect misplaced double quotes");
	}
	
	public void testAcceptEscapedQuotes() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote \\\"Code Execution\"; Set=Test}");
	}
	
	public void testParseWithAmbiguousIfSets() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=AVC; IfNotSet=Test1; IfSet=Test1; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect variable setting that is ambiguous");
	}
	
	public void testParseWithAmbiguousToggles() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=AVC; Set=Test1; Toggle=Test1; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect variable setting that is ambiguous");
	}
	
	public void testParseWithAmbiguousToggles2() throws UnpurposedDefinitionException{		
		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=AVC; UnSet=Test1; Toggle=Test1; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect variable setting that is ambiguous");
	}
	
	public void testActionCheck() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSignature = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=L33t;}");
		
		if( threatSignature.getAction() != PatternDefinition.Action.ALERT ){
			fail("The action was not identified properly");
		}
	}
	
	public void testNoCaseWithoutEvaluator() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; NoCase; String=L33t;}");
		}
		catch(InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with NoCase option before String evaluator");
		
	}
	
	public void testOnlySet() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Set=Test}");
	}
	
	public void testOnlySet2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Set=Test; IfSet=Test2}");
	}
	
	public void testUnpurposedRule() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\";}");
		}
		catch (UnpurposedDefinitionException e){
			return;
		}
		
		fail("Failed to identify unpurposed rule");
	}
	
	public void testInvalidID() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Set=TEST; Version=Err}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid version");
	}
	
	public void testInvalidVersion() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Set=TEST; ID=Err}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid ID");
	}
	
	public void testDepthAtEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition sig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=4;}");
		Variables vars = new Variables();
		
		if( sig.evaluate("Tree", vars) == false ){
			fail("Failed to match using depth just at edge");
		}
	}
	
	public void testDepthBeforeEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition sig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=4;}");
		Variables vars = new Variables();
		
		if( sig.evaluate(" Tree", vars) == true ){
			fail("Incorrectly matched using depth just past edge");
		}
	}

	public void testDepthPastEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition sig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=5;}");
		Variables vars = new Variables();
		
		if( sig.evaluate("Tree      ", vars) == false ){
			fail("Failed to match using depth just after matching region");
		}
	}
	
	public void testDepthPastStringToReview() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition sig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=18;}");
		Variables vars = new Variables();
		
		if( sig.evaluate("Tree", vars) == false ){
			fail("Failed to match with depth past string length");
		}
	}
	
	public void testInvalidNegativeDepth() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=-1;}");
			
		}
		catch( InvalidDefinitionException e ){
			return;
		}
		
		fail("Failed to identify signature with invalid depth (negative number)");
	}
	
	/*public void testDepthAfterNoEvaluator() throws InvalidSignatureException, UnpurposedRuleException{
		
		ThreatSignature sig = ThreatSignature.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=18;}");
		Variables vars = new Variables();
		
		if( sig.evaluate("Tree", vars) == false ){
			fail("Failed to match with depth past string length");
		}
	}*/
	
	public void testInvalidZeroDepth() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=0;}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with zero depth");
	}
	
	public void testInvalidDepth() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=Err;}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid depth");
	}
	
	public void testInvalidMultipleDepth() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Depth=13; Depth=14; }");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with multiple \"depth\" options");
	}
	
	public void testInvalidOutOfOrderDepth() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Depth=13; String=Test;}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid \"depth\" option (before evaluator)");
	}
	
	public void testInvalidWithin() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; String=Tree; Within=Err}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid within");
	}
	
	public void testInvalidOutOfOrderWithin() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Within=13; String=Tree;}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid \"within\" option(before evaluator)");
	}
	
	public void testInvalidOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message\"Remote Code Execution\"; String=Tree; Offset=Err}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid offset");
	}
	
	public void testInvalidOutOfOrderOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Offset=13; String=Tree;}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule with invalid \"offset\" option(before evaluator)");
	}
	
	public void testUnpurposedRule2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Evaluate(Test.Exec.Start){Severity=Low; Message=\"Remote Code Execution\"; Regex=\"/apple/i\"}");
		}
		catch (UnpurposedDefinitionException e){
			return;
		}
		
		fail("Failed to identify unpurposed rule");
	}
	
	public void testNoName() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Evaluate(){Severity=Low; Message=\"Remote Code Execution\"; Regex=\"/apple/i\"}");
		}
		catch (InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to identify rule without a name");
	}
	
	public void testMatchString() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=iframe}");
		Variables variables = new Variables();
		
		if( !threatSig.evaluate("<html><body>iframe</body></html>".getBytes(), variables) ){
			fail("Failed to identify data using string evaluator");
		}
	}
	//
	
	public void testMatchStringSetVar() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.CLSIDDetected){ Severity=Low; Message=\"Remote Code Execution\"; String=CLSID; Set=CLSID}");
		Variables variables = new Variables();
		
		if( !threatSig.evaluate("<html><body>CLSID</body></html>".getBytes(), variables) ){
			fail("Failed to identify data using string evaluator");
		}
		
		if( !variables.isSet("CLSID") ){
			fail("Variable was not properly set and should have been");
		}
	}
	
	public void testMatchStringSetVar2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"new ActiveXObject\"; Set=NewActiveXObject}");
		PatternDefinition threatSigMMC = PatternDefinition.parse("Alert(Exploit.RemoteCodeExecution.daxctle){Severity=Low; Message=\"Internet Explorer (daxctle.ocx) Heap Overflow Vulnerability\"; IfSet=NewActiveXObject; String=DirectAnimation.PathControl;}");
		
		Variables variables = new Variables();
		
		byte[] bytes = "<script>var target = new ActiveXObject(\"DirectAnimation.PathControl\");\n target.Spline(0xffffffff, 1);</script>".getBytes();
		
		if( !threatSig.evaluate(bytes, variables) ){
			fail("Failed to identify data using string evaluator");
		}
		
		if( !variables.isSet("NewActiveXObject") ){
			fail("Variable was not properly set and should have been");
		}
		
		if( !threatSigMMC.evaluate(bytes, variables) ){
			fail("Failed to identify data using string evaluator");
		}
	}
	
	public void testToggleVariable() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		
		Variables variables = new Variables();
		
		DataSpecimen data = new DataSpecimen("ABC");
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=ABC; Set=ABC;}");
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to identify content");
		}
		
		if( !variables.isSet("ABC") ){
			fail("Failed to set variable");
		}
		
		PatternDefinition threatSig2 = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=ABC; Toggle=ABC;}");
		
		if( !threatSig2.evaluate(data, variables) ){
			fail("Failed to identify content");
		}
		
		if( variables.isSet("ABC") ){
			fail("Failed to toggle variable");
		}
	}

	public void testParseWithinZero() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Within=0;}");
		}catch(InvalidDefinitionException e){
			return;
		}
		
		fail("Failed reject signature with within set to zero");
	}
	
	public void testParseOffsetAndDepthAreTooSmall() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"66 66 66 66\"; Offset=5; Depth=3;}");
		}catch(InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to reject evaluators whose depth option will never match");
	}
	
	public void testParseDepthAreTooSmall() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"66 66 66 66\"; Depth=3;}");
		}catch(InvalidDefinitionException e){
			return;
		}
		
		fail("Failed to reject evaluators whose depth option will never match");
	}
	
	public void testParseOffsetAndDepthAlmostTooSmall() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"66 66 66 66\"; Offset=4; Depth=4;}");
		}catch(InvalidDefinitionException e){
			fail("Failed to accept evaluators with both depth and offset options that barely allow matches");
		}
	}
	
	public void testParseDepthAlmostTooSmall() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"66 66 66 66\"; Depth=4;}");
		}catch(InvalidDefinitionException e){
			fail("Failed to accept evaluators with both depth option that barely allow matches");
		}
	}
	
	public void testParseWithinAndDepthAfterByte() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Within=2; Depth=3;}");
		}catch(InvalidDefinitionException e){
			fail("Failed to accept byte expression evaluator with both depth and within options");
		}
	}
	
	public void testParseWithinAndDepthAfterString() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; String=\"BBBB\"; Within=2; Depth=8;}");
		}catch(InvalidDefinitionException e){
			fail("Failed to accept string expression evaluator with both depth and within options");
		}
	}
	
	public void testParseWithinAndDepthAfterRegex() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Regex=/BBBB/i; Within=2; Depth=3;}");
		}catch(InvalidDefinitionException e){
			fail("Failed to accept regular expression evaluator with both a depth and within evaluator");
		}
	}
	
	public void testMatchByteOffsetDepthCalculation() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA   BBBB");
		byte[] bytes = data.getBasicEncodedString().getBytes();
		
		if( threatSig.evaluate(bytes, variables) == false ){
			fail("Failed to matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchByteOffsetDepthCalculation2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA    BBBB");
		byte[] bytes = data.getBasicEncodedString().getBytes();
		
		if( threatSig.evaluate(bytes, variables) == true ){
			fail("Incorrectly matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchStringOffsetDepthCalculation() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; String=\"B\"; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA   BBBB");
		
		if( threatSig.evaluate(data.getString(), variables) == false ){
			fail("Failed to matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchStringOffsetDepthCalculation2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; String=\"B\"; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA    BBBB");
		
		if( threatSig.evaluate(data.getString(), variables) == true ){
			fail("Incorrectly matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchRegexOffsetDepthCalculation() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; Regex=/B/; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA   BBBB");
		
		if( threatSig.evaluate(data.getString(), variables) == false ){
			fail("Failed to matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchRegexOffsetDepthCalculation2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; Regex=/B/; Offset=2; Depth=2;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA    BBBB");
		
		if( threatSig.evaluate(data.getString(), variables) == true ){
			fail("Incorrectly matched, evaluator did not calculate the offset correctly with the depth option");
		}
	}
	
	public void testMatchByteWithin() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA   BBBB");
		byte[] bytes = data.getBasicEncodedString().getBytes();
		
		if( threatSig.evaluate(bytes, variables) ){
			fail("Incorrectly matched, evaluator appears to have ignored within option after byte evaluator");
		}
	}
	
	public void testMatchByteWithinAtEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; BasicEncoding; String=\"AAAA\"; Byte=\"0x42\"; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		DataSpecimen data = new DataSpecimen("AAAA  BBBB");
		byte[] bytes = data.getBasicEncodedString().getBytes();
		
		if( threatSig.evaluate(bytes, variables) == false ){
			fail("Failed to match expression, evaluator appears to have miscalculated the \"within\" option after byte evaluator");
		}
	}
	
	public void testMatchRegexWithin() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; Regex=/B+/i; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA   BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) ){
			fail("Incorrectly matched, evaluator appears to have ignored within option after regex evaluator");
		}
	}
	
	public void testMatchRegexWithinAtEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=\"Test\"; String=\"AAAA\"; Regex=/B+/i; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA  BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) == false ){
			fail("Failed to match regular expression, evaluator appears to have miscalculated the \"within\" option after regex evaluator");
		}
	}
	
	public void testMatchStringWithinNoCase() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"AAAA\"; NoCase; String=BBBB; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA   BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) ){
			fail("Incorrectly matched, evaluator appears to have ignored within option");
		}
	}
	
	public void testMatchStringWithinAtEdgeNoCase() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"AAAA\"; NoCase; String=BBBB; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA  BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) == false ){
			fail("Evaluator should have matched");
		}
	}
	
	public void testMatchStringWithin() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"AAAA\"; String=BBBB; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA   BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) ){
			fail("Incorrectly matched, evaluator appears to have ignored within option");
		}
	}
	
	public void testMatchStringWithinAtEdge() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"AAAA\"; String=BBBB; Within=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "AAAA  BBBB</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) == false ){
			fail("Evaluator should have matched");
		}
	}
	
	public void testMatchStringOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"new ActiveXObject\"; Set=NewActiveXObject}");
		PatternDefinition threatSigMMC = PatternDefinition.parse("Alert(Exploit.RemoteCodeExecution.daxctle){Severity=Low; Message=\"Internet Explorer (daxctle.ocx) Heap Overflow Vulnerability\"; IfSet=NewActiveXObject; String=DirectAnimation.PathControl; Offset=41;}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "<script>var target = new ActiveXObject(\"DirectAnimation.PathControl\");\n target.Spline(0xffffffff, 1);</script>".getBytes();
		
		if( !threatSig.evaluate(bytes, variables) ){
			fail("Failed to identify data using string evaluator");
		}
		
		if( !variables.isSet("NewActiveXObject") ){
			fail("Variable was not properly set and should have been");
		}
		
		if( threatSigMMC.evaluate(bytes, variables) ){
			fail("The offset was not applied correctly (found data that should not been detected if offset was used)");
		}
	}
	
	public void testMatchStringOffset3() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"new ActiveXObject\"; String=DirectAnimation.PathControl; Offset=4}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "<script>var target = new ActiveXObject(\"DirectAnimation.PathControl\");\n target.Spline(0xffffffff, 1);</script>".getBytes();
		
		if( threatSig.evaluate(bytes, variables) ){
			fail("The offset was not applied correctly (found data that should not been detected if offset was used properly)");
		}
	}
	
	public void testMatchStringOffset2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ActiveXObject){Severity=Low; Message=\"ActiveXObject Instantiation Detected\"; String=\"new ActiveXObject\"; String=DirectAnimation.PathControl; Offset=2}");
		
		Variables variables = new Variables();
                     // 000000000011111111112222222222333333333 3444444444455555555556666666 6667
		             // 012345678901234567890123456789012345678 9012345678901234567890123456 7890
		byte[] bytes = "<script>var target = new ActiveXObject(\"DirectAnimation.PathControl\");\n target.Spline(0xffffffff, 1);</script>".getBytes();
		
		if( !threatSig.evaluate(bytes, variables) ){
			fail("Failed to identify data using string evaluator");
		}
	}
	
	public void testRegex() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Suspicious.ExcessiveJavascriptEscapes){Severity=Low; Message=\"Excessive JavaScript Unescapes\"; String=\"document.write\"; Regex=\"/unescape\\([a-zA-Z0-9%]{32,}\\)/\"; Offset=1}");
		
		Variables variables = new Variables();
		String data = "<script>document.write(unescape(%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90%90))</script>";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to identify data using string and regular expression evaluators");
		}
	}
	
	public void testBytes() throws InvalidDefinitionException, UnpurposedDefinitionException{
		//ThreatSignature threatSig = ThreatSignature.parse("Alert(Class=Exploit; Subclass=\"Remote Code Execution\"; Name=\"Generic Buffer Overflows\"; String=\"document.write\"; Byte=\"90 90 90 90 90 90 90 90 90 90 90 90 90 90 90\")");
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Exploit.Hueristics.x86ShellCodeNoopUnicode){Severity=Low; Message=\"Shellcode x86 0x90 unicode NOOPn\"; Byte=\"90 90 90 90 90 90 90 90 90 90\"}");
		
		Variables variables = new Variables();
		
		byte[] data = {65, 66, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112, -112};
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to identify data using byte evaluators");
		}
	}
	
	public void testStringAtStart() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; Message=\"No HTML Tag\"; String=\"<html>\";}");
		
		Variables variables = new Variables();
		String data = "<html><body>Test</body></html>";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Did not identify the string at the first position");
		}
	}
	
	public void testStringNegation() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; Message=\"No HTML Tag\"; String!=\"<html>\";}");
		
		Variables variables = new Variables();
		String data = "<html><body>Test</body></html>";
		
		if( threatSig.evaluate(data, variables) ){
			fail("Fired on a negated rule (should not have)");
		}
	}
	
	public void testStringNegation2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; Message=\"No HTML Tag\"; String!=\"<html>\";}");
		
		Variables variables = new Variables();
		String data = ".....<body>Test</body></html>";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to identify negated rule");
		}
	}
	
	public void testStringOverlap() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; Message=\"No HTML Tag\"; String=123; String=345; Offset=0;}");
		
		Variables variables = new Variables();
		String data = "12345";
		
		if( threatSig.evaluate(data, variables) ){
			fail("Double evaluated a character");
		}
	}
	
	public void testParseMultiline() throws InvalidDefinitionException, UnpurposedDefinitionException{
		try{
			PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; \n	Message=\"No HTML Tag\"; \n	String=123;\n	String=345;\n}");
		}
		catch(InvalidDefinitionException e){
			fail("Failed to parse a rule with multiple lines");
		}
	}
	
	public void testStringRegexMix() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Anomaly.DataFormat.MissingHtmlTag){Severity=Low; Message=\"No HTML Tag\"; String=123; Regex=/345/i;}");
		
		Variables variables = new Variables();
		String data = "123345";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Regex evaluator failed to properly detect data after String evaluator");
		}
	}
	
	
	/*
	 * -----------------------------------------------------------------------------------------------
	 * Mixed Mode Tests
	 * The tests below test the signatures in mixed-mode (byte evaluators and string evaluators).
	 * -----------------------------------------------------------------------------------------------
	 */
	public void testByteOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=123; Byte=\"41\";}");
		Variables variables = new Variables();
		
		String test = "123A";
		
		if( !threatSig.evaluate(test, variables) ){
			fail("Failed to identify content; likely due to misdetecting position due to string encoding and byte offsets");
		}
	}
	
	public void testUnicodeConversionByteOffset() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=123; Byte=\"00 42\"}");
		Variables variables = new Variables();
		
		byte[] bytes = "123ABC".getBytes("UTF-16");
		DataSpecimen data = new DataSpecimen(bytes);
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to identify content; likely due to misdetecting position due to string encoding and byte offsets");
		}
	}
	
	public void testBasicSequence() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=ABC; String=DEF; Offset=0;}");
		Variables variables = new Variables();
		
		byte[] bytes = "ABCDEF".getBytes();
		DataSpecimen data = new DataSpecimen(bytes);
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to detect rules in sequence");
		}
	}
	
	public void testIncorrectByteOffsetFromPreviousEvaluator() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; BasicEncoding; String=A; Byte=\"00 42\"; Offset=1;}");
		Variables variables = new Variables();
		
		byte[] bytes = "ABCCD".getBytes("UTF-16");
		DataSpecimen data = new DataSpecimen(bytes);
		
		if( threatSig.evaluate(data, variables) ){
			fail("Duplicate detection of same characters in two evaluators due to translation failure between string offset and byte offset (they are not guaranteed to be the same for non-ASCII encoding)");
		}
	}
	
	public void testBasicSequence2() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=ABC; String=DEF; Offset=0;}");
		Variables variables = new Variables();
		
		byte[] bytes = "DEFABC".getBytes();
		DataSpecimen data = new DataSpecimen(bytes);
		
		if( threatSig.evaluate(data, variables) ){
			fail("Failed to apply sequencing rules");
		}
	}
	
	public void testSequenceWithMisses() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=ABC; String=DEF; Offset=0; String=GHI; Offset=0; String=\"_:_\";}");
		Variables variables = new Variables();
		
		byte[] bytes = "ABCABCDEFABCDEF_:_GHIABCDEFGHI".getBytes();
		DataSpecimen data = new DataSpecimen(bytes);
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Failed to apply sequencing rules");
		}
	}
	
	public void testUnicodeConversionByteOffsetStringConv() throws InvalidDefinitionException, UnpurposedDefinitionException, UnsupportedEncodingException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Test.Test){Severity=Low; Message=Test; String=123; Byte=\"41\"}");
		Variables variables = new Variables();
		
		byte[] bytes = "123A".getBytes("UTF-16");
		DataSpecimen data = new DataSpecimen(bytes);
		
		String test = data.getString();
		
		if( !threatSig.evaluate(test, variables) ){
			fail("Failed to identify content; likely due to misdetecting position due to string encoding and byte offsets");
		}
	}
	
	public void testParseWithQuotedBracket() throws UnpurposedDefinitionException{		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=\"A}VC\";}");
		} catch (InvalidDefinitionException e) {
			fail("Failed to realize the ending bracket was in double quotes");
		}
	}
	
	public void testParseWithInvalidBracket() throws UnpurposedDefinitionException{		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=A}VC; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to realize that an ending bracket exists without being inside double quotes");
	}
	
	public void testParseWithMisplacedQuotes() throws UnpurposedDefinitionException{		
		try {
			PatternDefinition.parse("Alert(Test.Exec.Start){ Severity=Low; Message=\"Remote Code Execution\"; String=\"A\"VC\"; IfNotSet=Test1; IfSet=Test1; }");
		} catch (InvalidDefinitionException e) {
			return;
		}
		
		fail("Failed to detect misplaced double quotes");
	}
	
	public void testByteTestDigits() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; ByteTest=\"4 digits=256\";}");
		
		Variables variables = new Variables();
		String data = "256";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("ByteTest evaluator failed to properly match the data");
		}
	}
	
	public void testByteTestBytes() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; ByteTest=\"1 byte=13\";  Offset=1;}");
		
		Variables variables = new Variables();
		byte[] data = new byte[ 6 ];
		data[0] = 1;
		data[1] = 13;
		data[2] = 1;
		data[3] = 127;
		data[4] = 1;
		data[5] = 0;
		
		if( !threatSig.evaluate(data, variables) ){
			fail("ByteTest evaluator failed to properly match the data");
		}
	}
	
	public void testByteTestBytes2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; ByteTest=\"2 bytes=3329\"; Offset=1;}");
		
		Variables variables = new Variables();
		byte[] data = new byte[ 6 ];
		data[0] = 1;
		data[1] = 13;
		data[2] = 1;
		data[3] = 127;
		data[4] = 1;
		data[5] = 0;
		
		if( !threatSig.evaluate(data, variables) ){
			fail("ByteTest evaluator failed to properly match the data");
		}
	}
	
	public void testByteTestBytes3() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; ByteTest=\"2 bytes=383\"; Offset=2;}");
		
		Variables variables = new Variables();
		byte[] data = new byte[ 6 ];
		data[0] = 1;
		data[1] = 13;
		data[2] = 1;
		data[3] = 127;
		data[4] = 1;
		data[5] = 0;
		
		if( !threatSig.evaluate(data, variables) ){
			fail("ByteTest evaluator failed to properly match the data");
		}
	}
	
	public void testByteTestBytesUnsigned() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; ByteTest=\"2 bytes=65281(unsigned)\"; Offset=3;}");
		
		Variables variables = new Variables();
		byte[] data = new byte[ 6 ];
		data[0] = 1;
		data[1] = 13;
		data[2] = 1;
		data[3] = -128;
		data[4] = 1;
		data[5] = 0;
		
		if( !threatSig.evaluate(data, variables) ){
			fail("ByteTest evaluator failed to properly match the data");
		}
	}
	
	public void testIsDataAt() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; IsDataAt; Offset=3; String=L33t; BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33t";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("IsDataAt evaluator failed to properly detect data after String evaluator");
		}
	}
	
	public void testIsDataAt2() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; IsDataAt; Offset=3; String=L33t;}");
		
		Variables variables = new Variables();
		String data = "L33t";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("IsDataAt evaluator failed to properly detect data after String evaluator");
		}
	}
	
	public void testIsDataAt3() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t; IsDataAt; Offset=3; BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaa";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("IsDataAt evaluator failed to properly detect data after String evaluator");
		}
	}
	
	public void testIsDataAt4() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t; IsDataAt; Offset=5; BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaa";
		
		if( threatSig.evaluate(data, variables) ){
			fail("IsDataAt evaluator accepted input incorrectly");
		}
	}
	
	public void testIsDataAtShouldFail() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t; IsDataAt; Offset=6; BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaa";
		
		if( threatSig.evaluate(data, variables) ){
			fail("IsDataAt evaluator should not have accepted the input");
		}
	}
	
	public void testRelativeOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t; String=bbbb; Offset=4(relative); BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaabbbb";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Input should have been accepted");
		}
	}
	
	public void testAbsoluteOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=L33t; String=bbbb; Offset=8(absolute); BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaabbbb";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Input should have been accepted");
		}
	}
	
	public void testInvalidRelativeOffset() throws InvalidDefinitionException, UnpurposedDefinitionException{
		boolean parseFailed = false;
		
		try{
			PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=bbbb; Offset=8(relative); BasicEncoding;}");
		}
		catch(InvalidDefinitionException e){
			parseFailed = true;
		}
		
		if( parseFailed == false ){
			fail("Signature parse should have failed (has a relative offset with nothing to be relative too since a previous evaluator does not exist)");
		}
	}
	
	public void testAbsoluteOffsetFromBeginning() throws InvalidDefinitionException, UnpurposedDefinitionException{
		
		PatternDefinition threatSig = PatternDefinition.parse("Alert(Test.Exec.Start){Severity=Low; Message=Exploit; String=bbbb; Offset=8(absolute); BasicEncoding;}");
		
		Variables variables = new Variables();
		String data = "L33taaaabbbb";
		
		if( !threatSig.evaluate(data, variables) ){
			fail("Input should have been accepted");
		}
	}
}
