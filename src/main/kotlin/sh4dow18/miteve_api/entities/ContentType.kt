package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

// Genre Entity
@Entity
@Table(name = "content_types")
data class ContentType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @OneToMany(mappedBy = "type", targetEntity = Content::class)
    var contentsList: List<Content>,
)