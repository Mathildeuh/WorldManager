package fr.mathildeuh.worldmanager.configs;

/** One sign-portal binding: right-clicking the sign at ({@code world},{@code x},{@code y},{@code z}) sends the player to {@code destinationWorld}'s spawn. */
public record SignPortal(String id, String world, int x, int y, int z, String destinationWorld, String createdBy) {
}
