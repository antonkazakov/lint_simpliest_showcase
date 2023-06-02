package otus.demo.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.lang.Language
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UWhileExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor


private const val ID = "SimpleNameUsage"
private const val BRIEF_DESCRIPTION =
    "Замените simpleName на canonicalName или на конкретное имя класса"
private const val EXPLANATION =
    "Замените simpleName на canonicalName или на конкретное имя класса. " +
            "simpleName может возвращать неожидаемый результат после минификации"

private const val PRIORITY = 6

private const val CLASS = "java.lang.Class"

private const val SIMPLE_NAME_KOTLIN_METHOD = "simpleName"
private const val SIMPLE_NAME_JAVA_METHOD = "getSimpleName"
private const val CANONICAL_NAME_KOTLIN_METHOD = "canonicalName"
private const val CANONICAL_NAME_JAVA_METHOD = "getCanonicalName"

/**
 * String::class.java.simpleName
 * String.class.getSimpleName()
 */

class GetClassSimpleNameDetector : Detector(), Detector.UastScanner {

    companion object {

        val ISSUE = Issue.create(
            ID,
            BRIEF_DESCRIPTION,
            EXPLANATION,
            Category.create("TEST CATEGORY", 10),
            PRIORITY,
            Severity.WARNING,
            Implementation(GetClassSimpleNameDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("getSimpleName")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        super.visitMethodCall(context, node, method)

        if (node.receiverType?.canonicalText?.contains(CLASS) != true) return


        val psiElement = node.sourcePsi

        val className = psiElement?.getParentOfType<KtClass>(true)?.name


        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            BRIEF_DESCRIPTION,
            createFix(isKotlin(psiElement), className, context.getLocation(node))
        )
    }

    private fun createFix(isKotlin: Boolean, className: String?, location: Location): LintFix {
        return if (isKotlin) {
            createKotlinFix(className, location)
        } else {
            createJavaFix(className, location)
        }
    }

    private fun createKotlinFix(className: String?, location: Location): LintFix {
        if (className == null) return replaceWithCanonicalNameKotlin()
        return fix().alternatives(
            replaceWithCanonicalNameKotlin(),
            createReplaceFix(location, className)
        )
    }

    private fun replaceWithCanonicalNameKotlin(): LintFix {
        return fix().replace()
            .text(SIMPLE_NAME_KOTLIN_METHOD)
            .with("$CANONICAL_NAME_KOTLIN_METHOD!!")
            .build()
    }

    private fun createReplaceFix(
        location: Location,
        className: String?
    ): LintFix {
        return fix().replace()
            .range(location)
            .with("\"$className\"")
            .build()
    }

    private fun createJavaFix(className: String?, location: Location): LintFix {
        if (className == null) return replaceWithCanonicalNameJava()
        return fix().alternatives(
            replaceWithCanonicalNameJava(),
            createReplaceFix(location, className)
        )
    }

    private fun replaceWithCanonicalNameJava(): LintFix {
        return fix().replace()
            .text(SIMPLE_NAME_JAVA_METHOD)
            .with(CANONICAL_NAME_JAVA_METHOD)
            .build()
    }
}