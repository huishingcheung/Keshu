package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.data.repository.AdvisorRepository
import edu.jnu.smartedu.domain.model.CourseAdvice

class GetCourseAdviceUseCase(private val repository: AdvisorRepository) {
    suspend operator fun invoke(apiKey: String): CourseAdvice {
        require(apiKey.isNotBlank()) { "请输入 DeepSeek API Key" }
        return repository.requestAdvice(apiKey)
    }
}
