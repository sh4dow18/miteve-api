package sh4dow18.miteve_api.entities

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table

// Container Entity
@Entity
@Table(name = "containers")
data class Container(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @OneToMany(mappedBy = "container", targetEntity = ContainerElement::class)
    @OrderBy("position ASC")
    var elementsList: List<ContainerElement>
)