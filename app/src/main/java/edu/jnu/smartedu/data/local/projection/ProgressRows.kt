package edu.jnu.smartedu.data.local.projection

data class SmallGroupProgressRow(
    val groupId: String,
    val largeGroupId: String,
    val parentSmallGroupId: String?,
    val depth: Int,
    val name: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val met: Boolean,
)

data class LargeGroupProgressRow(
    val groupId: String,
    val name: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val smallGroupCount: Int,
    val metSmallGroupCount: Int,
    val met: Boolean,
)

data class GraduationProgressRow(
    val requiredCredits: Double,
    val earnedCredits: Double,
    val largeGroupCount: Int,
    val metLargeGroupCount: Int,
    val met: Boolean,
)
