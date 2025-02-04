



import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper.DiagnosticSourceUnwrapper;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.util.JCDiagnostic;

import java.io.File;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static javax.tools.StandardLocation.*;

public class ResolveHarness implements javax.tools.DiagnosticListener<JavaFileObject> {

    static int nerrors = 0;

    static final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    static final StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);

    public static void main(String[] args) throws Exception {
        fm.setLocation(SOURCE_PATH,
                Arrays.asList(new File(System.getProperty("test.src"), "tests")));
        for (JavaFileObject jfo : fm.list(SOURCE_PATH, "", Collections.singleton(JavaFileObject.Kind.SOURCE), true)) {
            new ResolveHarness(jfo).check();
        }
        if (nerrors > 0) {
            throw new AssertionError("Errors were found");
        }
    }


    JavaFileObject jfo;
    DiagnosticProcessor[] diagProcessors;
    Map<ElementKey, Candidate> candidatesMap = new HashMap<ElementKey, Candidate>();
    Set<String> declaredKeys = new HashSet<>();
    List<Diagnostic<? extends JavaFileObject>> diags = new ArrayList<>();
    List<ElementKey> seenCandidates = new ArrayList<>();
    Map<String, String> predefTranslationMap = new HashMap<>();

    protected ResolveHarness(JavaFileObject jfo) {
        this.jfo = jfo;
        this.diagProcessors = new DiagnosticProcessor[] {
            new VerboseResolutionNoteProcessor(),
            new VerboseDeferredInferenceNoteProcessor(),
            new ErrorProcessor()
        };
        predefTranslationMap.put("+", "_plus");
        predefTranslationMap.put("-", "_minus");
        predefTranslationMap.put("~", "_not");
        predefTranslationMap.put("++", "_plusplus");
        predefTranslationMap.put("--", "_minusminus");
        predefTranslationMap.put("!", "_bang");
        predefTranslationMap.put("*", "_mul");
        predefTranslationMap.put("/", "_div");
        predefTranslationMap.put("%", "_mod");
        predefTranslationMap.put("&", "_and");
        predefTranslationMap.put("|", "_or");
        predefTranslationMap.put("^", "_xor");
        predefTranslationMap.put("<<", "_lshift");
        predefTranslationMap.put(">>", "_rshift");
        predefTranslationMap.put("<<<", "_lshiftshift");
        predefTranslationMap.put(">>>", "_rshiftshift");
        predefTranslationMap.put("<", "_lt");
        predefTranslationMap.put(">", "_gt");
        predefTranslationMap.put("<=", "_lteq");
        predefTranslationMap.put(">=", "_gteq");
        predefTranslationMap.put("==", "_eq");
        predefTranslationMap.put("!=", "_neq");
        predefTranslationMap.put("&&", "_andand");
        predefTranslationMap.put("||", "_oror");
    }

    protected void check() throws Exception {
        String[] options = {
            "-XDshouldStopPolicy=ATTR",
            "-XDverboseResolution=success,failure,applicable,inapplicable,deferred-inference,predef"
        };

        AbstractProcessor[] processors = { new ResolveCandidateFinder(), null };

        @SuppressWarnings("unchecked")
        DiagnosticListener<? super JavaFileObject>[] diagListeners =
                new DiagnosticListener[] { new DiagnosticHandler(false), new DiagnosticHandler(true) };

        for (int i = 0 ; i < options.length ; i ++) {
            JavacTask ct = (JavacTask)comp.getTask(null, fm, diagListeners[i],
                    Arrays.asList(options[i]), null, Arrays.asList(jfo));
            if (processors[i] != null) {
                ct.setProcessors(Collections.singleton(processors[i]));
            }
            ct.analyze();
        }

        
        for (Diagnostic<? extends JavaFileObject> diag : diags) {
            for (DiagnosticProcessor proc : diagProcessors) {
                if (proc.matches(diag)) {
                    proc.process(diag);
                    break;
                }
            }
        }
        
        for (Map.Entry<ElementKey, Candidate> entry : candidatesMap.entrySet()) {
            if (!seenCandidates.contains(entry.getKey())) {
                error("Redundant @Candidate annotation on method " + entry.getKey().elem + " sig = " + entry.getKey().elem.asType());
            }
        }
    }

    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        diags.add(diagnostic);
    }

    Candidate getCandidateAtPos(Element methodSym, long line, long col) {
        Candidate c = candidatesMap.get(new ElementKey(methodSym));
        if (c != null) {
            Pos pos = c.pos();
            if (!pos.userDefined() ||
                    (pos.line() == line && pos.col() == col)) {
                seenCandidates.add(new ElementKey(methodSym));
                return c;
            }
        } else {
            error("Missing @Candidate annotation on method " + methodSym);
        }
        return null;
    }

    void checkSig(Candidate c, Element methodSym, MethodType mtype) {
        if (c.sig().length() > 0 && !c.sig().equals(mtype.toString())) {
            error("Inferred type mismatch for method: " + methodSym);
        }
    }

    protected void error(String msg) {
        nerrors++;
        System.err.printf("Error occurred while checking file: %s\nreason: %s\n", jfo.getName(), msg);
    }

    
    abstract class DiagnosticProcessor {

        List<String> codes;
        Diagnostic.Kind kind;

        public DiagnosticProcessor(Kind kind, String... codes) {
            this.codes = Arrays.asList(codes);
            this.kind = kind;
        }

        abstract void process(Diagnostic<? extends JavaFileObject> diagnostic);

        boolean matches(Diagnostic<? extends JavaFileObject> diagnostic) {
            return (codes.isEmpty() || codes.contains(diagnostic.getCode())) &&
                    diagnostic.getKind() == kind;
        }

        JCDiagnostic asJCDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic instanceof JCDiagnostic) {
                return (JCDiagnostic)diagnostic;
            } else if (diagnostic instanceof DiagnosticSourceUnwrapper) {
                return ((DiagnosticSourceUnwrapper)diagnostic).d;
            } else {
                throw new AssertionError("Cannot convert diagnostic to JCDiagnostic: " + diagnostic.getClass().getName());
            }
        }

        List<JCDiagnostic> subDiagnostics(Diagnostic<? extends JavaFileObject> diagnostic) {
            JCDiagnostic diag = asJCDiagnostic(diagnostic);
            if (diag instanceof JCDiagnostic.MultilineDiagnostic) {
                return ((JCDiagnostic.MultilineDiagnostic)diag).getSubdiagnostics();
            } else {
                throw new AssertionError("Cannot extract subdiagnostics: " + diag.getClass().getName());
            }
        }
    }

    
    class VerboseResolutionNoteProcessor extends DiagnosticProcessor {

        VerboseResolutionNoteProcessor() {
            super(Kind.NOTE,
                    "compiler.note.verbose.resolve.multi",
                    "compiler.note.verbose.resolve.multi.1");
        }

        @Override
        void process(Diagnostic<? extends JavaFileObject> diagnostic) {
            Element siteSym = getSiteSym(diagnostic);
            if (siteSym.getSimpleName().length() != 0 &&
                    ((Symbol)siteSym).outermostClass().getAnnotation(TraceResolve.class) == null) {
                return;
            }
            int candidateIdx = 0;
            for (JCDiagnostic d : subDiagnostics(diagnostic)) {
                boolean isMostSpecific = candidateIdx++ == mostSpecific(diagnostic);
                VerboseCandidateSubdiagProcessor subProc =
                        new VerboseCandidateSubdiagProcessor(isMostSpecific, phase(diagnostic), success(diagnostic));
                if (subProc.matches(d)) {
                    subProc.process(d);
                } else {
                    throw new AssertionError("Bad subdiagnostic: " + d.getCode());
                }
            }
        }

        Element getSiteSym(Diagnostic<? extends JavaFileObject> diagnostic) {
            return (Element)asJCDiagnostic(diagnostic).getArgs()[1];
        }

        int mostSpecific(Diagnostic<? extends JavaFileObject> diagnostic) {
            return success(diagnostic) ?
                    (Integer)asJCDiagnostic(diagnostic).getArgs()[2] : -1;
        }

        boolean success(Diagnostic<? extends JavaFileObject> diagnostic) {
            return diagnostic.getCode().equals("compiler.note.verbose.resolve.multi");
        }

        Phase phase(Diagnostic<? extends JavaFileObject> diagnostic) {
            return Phase.fromString(asJCDiagnostic(diagnostic).getArgs()[3].toString());
        }
    }

    
    class VerboseCandidateSubdiagProcessor extends DiagnosticProcessor {

        boolean mostSpecific;
        Phase phase;
        boolean success;

        public VerboseCandidateSubdiagProcessor(boolean mostSpecific, Phase phase, boolean success) {
            super(Kind.OTHER,
                    "compiler.misc.applicable.method.found",
                    "compiler.misc.applicable.method.found.1",
                    "compiler.misc.not.applicable.method.found");
            this.mostSpecific = mostSpecific;
            this.phase = phase;
            this.success = success;
        }

        @Override
        void process(Diagnostic<? extends JavaFileObject> diagnostic) {
            Symbol methodSym = (Symbol)methodSym(diagnostic);
            if ((methodSym.flags() & Flags.GENERATEDCONSTR) != 0) {
                
                return;
            }
            Candidate c = getCandidateAtPos(methodSym,
                    asJCDiagnostic(diagnostic).getLineNumber(),
                    asJCDiagnostic(diagnostic).getColumnNumber());
            if (c == null) {
                return; 
            }

            if (c.applicable().length == 0 && c.mostSpecific()) {
                error("Inapplicable method cannot be most specific " + methodSym);
            }

            if (isApplicable(diagnostic) != Arrays.asList(c.applicable()).contains(phase)) {
                error("Invalid candidate's applicability " + methodSym);
            }

            if (success) {
                for (Phase p : c.applicable()) {
                    if (phase.ordinal() < p.ordinal()) {
                        error("Invalid phase " + p + " on method " + methodSym);
                    }
                }
            }

            if (Arrays.asList(c.applicable()).contains(phase)) { 
                if (c.mostSpecific() != mostSpecific) {
                    error("Invalid most specific value for method " + methodSym + " " + new ElementKey(methodSym).key);
                }
                MethodType mtype = getSig(diagnostic);
                if (mtype != null) {
                    checkSig(c, methodSym, mtype);
                }
            }
        }

        boolean isApplicable(Diagnostic<? extends JavaFileObject> diagnostic) {
            return !diagnostic.getCode().equals("compiler.misc.not.applicable.method.found");
        }

        Element methodSym(Diagnostic<? extends JavaFileObject> diagnostic) {
            return (Element)asJCDiagnostic(diagnostic).getArgs()[1];
        }

        MethodType getSig(Diagnostic<? extends JavaFileObject> diagnostic) {
            JCDiagnostic details = (JCDiagnostic)asJCDiagnostic(diagnostic).getArgs()[2];
            if (details == null) {
                return null;
            } else if (details instanceof JCDiagnostic) {
                return details.getCode().equals("compiler.misc.full.inst.sig") ?
                        (MethodType)details.getArgs()[0] : null;
            } else {
                throw new AssertionError("Bad diagnostic arg: " + details);
            }
        }
    }

    
    class VerboseDeferredInferenceNoteProcessor extends DiagnosticProcessor {

        public VerboseDeferredInferenceNoteProcessor() {
            super(Kind.NOTE, "compiler.note.deferred.method.inst");
        }

        @Override
        void process(Diagnostic<? extends JavaFileObject> diagnostic) {
            Element methodSym = methodSym(diagnostic);
            Candidate c = getCandidateAtPos(methodSym,
                    asJCDiagnostic(diagnostic).getLineNumber(),
                    asJCDiagnostic(diagnostic).getColumnNumber());
            MethodType sig = sig(diagnostic);
            if (c != null && sig != null) {
                checkSig(c, methodSym, sig);
            }
        }

        Element methodSym(Diagnostic<? extends JavaFileObject> diagnostic) {
            return (Element)asJCDiagnostic(diagnostic).getArgs()[0];
        }

        MethodType sig(Diagnostic<? extends JavaFileObject> diagnostic) {
            return (MethodType)asJCDiagnostic(diagnostic).getArgs()[1];
        }
    }

    
    class ErrorProcessor extends DiagnosticProcessor {

        public ErrorProcessor() {
            super(Diagnostic.Kind.ERROR);
        }

        @Override
        void process(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (!declaredKeys.contains(diagnostic.getCode())) {
                error("Unexpected compilation error key '" + diagnostic.getCode() + "'");
            }
        }
    }

    @SupportedAnnotationTypes({"Candidate","TraceResolve"})
    class ResolveCandidateFinder extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver())
                return true;

            TypeElement traceResolveAnno = elements.getTypeElement("TraceResolve");
            TypeElement candidateAnno = elements.getTypeElement("Candidate");

            if (!annotations.contains(traceResolveAnno)) {
                error("no @TraceResolve annotation found in test class");
            }

            if (!annotations.contains(candidateAnno)) {
                error("no @candidate annotation found in test class");
            }

            for (Element elem: roundEnv.getElementsAnnotatedWith(traceResolveAnno)) {
                TraceResolve traceResolve = elem.getAnnotation(TraceResolve.class);
                declaredKeys.addAll(Arrays.asList(traceResolve.keys()));
            }

            for (Element elem: roundEnv.getElementsAnnotatedWith(candidateAnno)) {
                candidatesMap.put(new ElementKey(elem), elem.getAnnotation(Candidate.class));
            }
            return true;
        }
    }

    class ElementKey {

        String key;
        Element elem;

        public ElementKey(Element elem) {
            this.elem = elem;
            this.key = computeKey(elem);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ElementKey) {
                ElementKey other = (ElementKey)obj;
                return other.key.equals(key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        String computeKey(Element e) {
            String simpleName = e.getSimpleName().toString();
            String opName = predefTranslationMap.get(simpleName);
            String name = opName != null ? opName : simpleName;
            return name + e.asType();
        }

        @Override
        public String toString() {
            return "Key{"+key+"}";
        }
    }

    class DiagnosticHandler implements DiagnosticListener<JavaFileObject> {

        boolean shouldRecordDiags;

        DiagnosticHandler(boolean shouldRecordDiags) {
            this.shouldRecordDiags = shouldRecordDiags;
        }

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (shouldRecordDiags)
                diags.add(diagnostic);
        }

    }
}
