package sh4dow18.miteve_api.entities

import jakarta.persistence.*

@Entity
@Table(name = "privileges")
class Privilege(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    @ManyToMany(mappedBy = "privilegesList", fetch = FetchType.LAZY, targetEntity = Role::class)
    var rolesList: Set<Role>
) {
    override fun equals(other: Any?): Boolean {
        // Check if the current object is the same instance as other
        if (this === other) return true
        // Check if other is a Privilege
        if (other !is Privilege) return false
        // Compare the id of this object with the id of the other object
        return id == other.id
    }
    // Use the hashCode of the "id" field as the hash code for the entire object
    override fun hashCode(): Int = id.hashCode()
}