package org.perses.grammar.wgsl;

import com.google.common.primitives.ImmutableIntArray;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.perses.grammar.AbstractDefaultParserFacade;

public final class WgslParserFacade
        extends AbstractDefaultParserFacade<PnfWGSLLexer, PnfWGSLParser> {

    public WgslParserFacade() {
        super(
                LanguageWgsl.INSTANCE,
                createCombinedAntlrGrammar("PnfWGSL.g4", WgslParserFacade.class),
                PnfWGSLLexer.class,
                PnfWGSLParser.class,
                ImmutableIntArray.of(PnfWGSLLexer.IDENT));
    }

    @Override
    protected PnfWGSLLexer createLexer(CharStream inputStream) {
        return new PnfWGSLLexer(inputStream);
    }

    @Override
    protected PnfWGSLParser createParser(CommonTokenStream tokens) {
        return new PnfWGSLParser(tokens);
    }

    @Override
    protected ParseTree startParsing(PnfWGSLParser parser) {
        return parser.translation_unit();
    }
}
