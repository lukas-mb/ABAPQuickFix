package com.abapblog.adt.quickfix.assist.syntax.statements.combine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.abapblog.adt.quickfix.assist.syntax.codeParser.AbapStatement;
import com.abapblog.adt.quickfix.assist.syntax.codeParser.StringCleaner;
import com.abapblog.adt.quickfix.assist.syntax.statements.IAssistRegex;
import com.abapblog.adt.quickfix.assist.syntax.statements.StatementAssist;

public class Data extends StatementAssist implements IAssistRegex {

	private static final String BeginningOfStatement = "\r\n\tDATA: ";
	private static final String NewLineWithTabAndSpaceString = "\r\n\t  ";
	private static final String NewLineString = "\r\n";
	private static final String NewLinePattern = "\\r\\n";
	private static final String NewLinePatternWithSpaces = "\\r\\n\\s*";
	private String MatchPattern = "(?s)\\s*data\\s*:*\\s+(.*)";
	private String InlineDeclarationAtBeginning = "(?s)data\\(\\w*\\)\\s+=";
	private String InlineDeclaration = "(?s)data\\s*:*\\s+(.*)=";
	private String ReplacePattern = "$1";
	private boolean assistWithNext;
	private boolean assistWithPrevious;
	private List<AbapStatement> matchedStatements;

	public Data() {
		super();
	}

	@Override
	public String getMatchPattern() {
		return MatchPattern;
	}

	@Override
	public String getReplacePattern() {
		return ReplacePattern;
	}

	@Override
	public String getChangedCode() {
		String multipleEmptyLines = "(\\r?\\n?(\\r?\\n))+";
		String ChangedCode = "";
		if (assistWithPrevious == true || assistWithNext == true) {

			checkPreviousStatements(CodeReader.CurrentStatement);
			matchedStatements.add(CodeReader.CurrentStatement);
			checkNextStatements(CodeReader.CurrentStatement);

			Iterator<AbapStatement> statementIterator = matchedStatements.iterator();
			while (statementIterator.hasNext()) {
				AbapStatement statement = statementIterator.next();
				if (statement == matchedStatements.get(0)) {
					ChangedCode = statement.replacePattern(getMatchPattern(), getReplacePattern())
							.replaceAll(multipleEmptyLines, NewLineString).replaceFirst(NewLinePatternWithSpaces, "");
					ChangedCode = BeginningOfStatement + ChangedCode;
				} else {
					ChangedCode = ChangedCode + statement.replacePattern(getMatchPattern(), getReplacePattern())
							.replaceAll(multipleEmptyLines, NewLineString)
							.replaceFirst(NewLinePattern, NewLineWithTabAndSpaceString);

				}
				if (statementIterator.hasNext())
					ChangedCode = ChangedCode + "," + NewLineWithTabAndSpaceString;
			}

		}

		return StringCleaner.clean(ChangedCode + ".");
	}

	@Override
	public String getAssistShortText() {
		return "Combine DATA statements";
	}

	@Override
	public String getAssistLongText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canAssist() {
		if (CodeReader.CurrentStatement.isFullLineComment())
			return false;
		canAssistWithNext();
		canAssistWithPrevious();
		if (assistWithPrevious || assistWithNext) {
			return true;
		}
		return false;
	}

	private boolean checkMatch(AbapStatement statement) {
		if (statement.matchPattern(getMatchPattern()) && !statement.matchPattern(InlineDeclaration)
				&& !statement.matchPattern(InlineDeclarationAtBeginning)) {
			return true;
		}
		return false;
	}

	private boolean canAssistWithPrevious() {
		if (CodeReader.PreviousStatement == null)
			return false;
		if (checkMatch(CodeReader.CurrentStatement) && checkMatch(CodeReader.PreviousStatement))
			assistWithPrevious = true;
		return assistWithPrevious;
	}

	private boolean canAssistWithNext() {
		if (CodeReader.NextStatement == null)
			return false;
		if (checkMatch(CodeReader.CurrentStatement) && checkMatch(CodeReader.NextStatement))
			assistWithNext = true;
		return assistWithNext;
	}

	@Override
	public int getStartOfReplace() {
		return matchedStatements.get(0).getBeginOfStatement();
	}

	@Override
	public int getReplaceLength() {
		int lastItem = matchedStatements.size() - 1;
		return matchedStatements.get(lastItem).getEndOfStatement() - getStartOfReplace() + 1;
	}

	public void checkPreviousStatements(AbapStatement statement) {
		if (matchedStatements == null)
			matchedStatements = new ArrayList<>();
		try {
			AbapStatement previousStatement = statement.getPreviousAbapStatement();
			if (checkMatch(previousStatement)) {
				if (previousStatement.isFullLineComment() == false)
					matchedStatements.add(0, previousStatement);
				checkPreviousStatements(previousStatement);
			}
		} catch (Exception e) {

		}
	}

	public void checkNextStatements(AbapStatement statement) {
		if (matchedStatements == null)
			matchedStatements = new ArrayList<>();
		try {
			AbapStatement nextStatement = statement.getNextAbapStatement();
			if (checkMatch(nextStatement)) {
				if (nextStatement.isFullLineComment() == false)
					matchedStatements.add(nextStatement);
				checkNextStatements(nextStatement);
			}
		} catch (Exception e) {

		}
	}

}
