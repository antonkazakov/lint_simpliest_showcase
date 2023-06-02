package otus.demo.checks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class GetaClassSimpleNameDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GetClassSimpleNameDetector.ISSUE)

    @Test
    fun `should detect simpleName usage in kotlin`() {
        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                package test.pkg
                
                class Test2 {
                    
                     fun getClassName(): String {
                        return Test2::class.java.simpleName
                    }
                }               
            """.trimIndent()
                )
            )
            .run()
            .expect(
                """src/test/pkg/Test2.kt:6: Warning: Замените simpleName на canonicalName или на конкретное имя класса [SimpleNameUsage]
        return Test2::class.java.simpleName
               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings""".trimIndent()
            )
    }

    @Test
    fun `should detect getSimpleName in java`() {
        lintTask
            .files(
                LintDetectorTest.java(
                    """
                package test.pkg;
                
                public class Test1 {
                    
                    public class Test2 {
                    
                        String getClassName() {
                            return Test1.class.getSimpleName();
                        }
                    }
                }               
            """.trimIndent()
                )
            )
            .run()
            .expect(
                """
                src/test/pkg/Test1.java:8: Warning: Замените simpleName на canonicalName или на конкретное имя класса [SimpleNameUsage]
            return Test1.class.getSimpleName();
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """.trimIndent()
            )
    }

    @Test
    fun `should not detect for java custom objects`() {
        lintTask
            .files(
                LintDetectorTest.java(
                    """
                package test.pkg;
                
                public class Test1 {
                    
                    void invoke() {
                        Test2 test = new Test();
                        test.getSimpleName();
                    }
                    
                    public class Test2 {
                    
                        String getSimpleName() {
                            return "SimpleName";
                        }
                    }
                }               
            """.trimIndent()
                )
            )
            .run()
            .expect("No warnings.")
    }

    @Test
    fun `should not detect for kotlin custom objects`() {
        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                    package test.pkg
                
                    class Test1 {
                        val name = Test2().simpleName
                        val name2 = 5.simpleName
                    }
                    
                    class Test2 {
                        
                        fun getSimpleName(): String {
                            return "SimpleName"
                        }
                    }
                    
                    fun Int.simpleName(): String {
                        return "Number"
                    }
                """.trimIndent()
                )
            )
            .run()
            .expect("No warnings.")
    }
}