package sh4dow18.miteve_api.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.ZonedDateTime

// Content Entity
@Entity
@Table(name = "contents")
data class Content(
    @Id
    var id: String,
    var title: String,
    var year: Short,
    var tagline: String?,
    @Column(length = 1000)
    var description: String?,
    var rating: Float,
    var age: Short,
    var cover: String,
    var background: String,
    var trailer: String,
    var trailerDuration: Int,
    var comingSoon: Boolean,
    var createdDate: ZonedDateTime,
    @Column(length = 500)
    var note: String?,
    @ManyToMany(targetEntity = Genre::class)
    @JoinTable(
        name = "content_genre",
        joinColumns = [JoinColumn(name = "content_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "genre_id", referencedColumnName = "id")]
    )
    var genresList: Set<Genre>,
    @OneToOne(mappedBy = "content", targetEntity = ContainerElement::class)
    var containerElement: ContainerElement?,
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false, referencedColumnName = "id")
    var type: ContentType,
    @OneToMany(mappedBy = "content", targetEntity = Season::class)
    var seasonsList: List<Season>,
) {
    override fun equals(other: Any?): Boolean {
        // Check if the current object is the same instance as other
        if (this === other) return true
        // Check if other is a Content
        if (other !is Content) return false
        // Compare the id of this object with the id of the other object
        return id == other.id
    }
    // Use the hashCode of the "id" field as the hash code for the entire object
    override fun hashCode(): Int = id.hashCode()
}