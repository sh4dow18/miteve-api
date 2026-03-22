package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

// Container Element Entity
@Entity
@Table(name = "container_elements")
data class ContainerElement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var position: Short,
    @ManyToOne
    @JoinColumn(name = "container_id", nullable = false, referencedColumnName = "id")
    var container: Container,
    @OneToOne
    @JoinColumn(name = "content_id", nullable = true, referencedColumnName = "id")
    var content: Content?,
)