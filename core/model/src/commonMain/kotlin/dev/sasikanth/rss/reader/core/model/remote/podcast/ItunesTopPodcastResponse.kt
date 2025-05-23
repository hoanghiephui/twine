package dev.sasikanth.rss.reader.core.model.remote.podcast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class ItunesTopPodcastResponse(@SerialName("feed") val feed: Feed? = null)

@Serializable
data class Id(
  @SerialName("label") val label: String? = null,
  @SerialName("attributes") val attributes: Attributes? = null
)

@Serializable
data class ImReleaseDate(
  @SerialName("attributes") val attributes: Attributes? = null,
  @SerialName("label") val label: String? = null
)

@Serializable
data class Author(
  @SerialName("name") val name: Name? = null,
  @SerialName("uri") val uri: Uri? = null
)

@Serializable data class ImName(@SerialName("label") val label: String? = null)

@Serializable
data class Attributes(
  @SerialName("label") val label: String? = null,
  @SerialName("scheme") val scheme: String? = null,
  @SerialName("term") val term: String? = null,
  @SerialName("im:id") val imId: String? = null,
  @SerialName("rel") val rel: String? = null,
  @SerialName("href") val href: String? = null,
  @SerialName("type") val type: String? = null,
  @SerialName("amount") val amount: String? = null,
  @SerialName("currency") val currency: String? = null,
  @SerialName("height") val height: String? = null
)

@Serializable data class LinkItem(@SerialName("attributes") val attributes: Attributes? = null)

@Serializable data class Rights(@SerialName("label") val label: String? = null)

@Serializable data class Category(@SerialName("attributes") val attributes: Attributes? = null)

@Serializable
data class EntryItem(
  @SerialName("summary") val summary: Summary? = null,
  @SerialName("im:artist") val imArtist: ImArtist? = null,
  @SerialName("im:name") val imName: ImName? = null,
  @SerialName("im:contentType") val imContentType: ImContentType? = null,
  @SerialName("im:image") val imImage: List<ImImageItem>? = null,
  @SerialName("rights") val rights: Rights? = null,
  @SerialName("im:price") val imPrice: ImPrice? = null,
  @SerialName("link") val link: Link? = null,
  @SerialName("id") val id: Id? = null,
  @SerialName("title") val title: Title? = null,
  @SerialName("category") val category: Category? = null,
  @SerialName("im:releaseDate") val imReleaseDate: ImReleaseDate? = null,
)

@Serializable data class Icon(@SerialName("label") val label: String? = null)

@Serializable
data class Feed(
  @SerialName("entry") val entry: List<EntryItem>? = null,
  @SerialName("author") val author: Author? = null,
  @SerialName("rights") val rights: Rights? = null,
  @SerialName("icon") val icon: Icon? = null,
  @SerialName("link") val link: List<LinkItem>? = null,
  @SerialName("id") val id: Id? = null,
  @SerialName("title") val title: Title? = null,
  @SerialName("updated") val updated: Updated? = null
)

@Serializable data class Title(@SerialName("label") val label: String? = null)

@Serializable data class Updated(@SerialName("label") val label: String? = null)

@Serializable
data class ImArtist(
  @SerialName("attributes") val attributes: Attributes? = null,
  @SerialName("label") val label: String? = null
)

@Serializable
data class ImImageItem(
  @SerialName("attributes") val attributes: Attributes? = null,
  @SerialName("label") val label: String? = null
)

@Serializable data class Name(@SerialName("label") val label: String? = null)

@Serializable
data class ImContentType(@SerialName("attributes") val attributes: Attributes? = null)

@Serializable
data class ImPrice(
  @SerialName("attributes") val attributes: Attributes? = null,
  @SerialName("label") val label: String? = null
)

@Serializable data class Summary(@SerialName("label") val label: String? = null)

@Serializable data class Link(@SerialName("attributes") val attributes: Attributes? = null)

@Serializable data class Uri(@SerialName("label") val label: String? = null)

data class FeedModel(val title: String, val imImage: String, val imArtist: String)

fun EntryItem.toFeedModel(): FeedModel =
  FeedModel(
    title = title?.label ?: "Un",
    imArtist = imArtist?.label ?: "Un",
    imImage = imImage?.last()?.label ?: ""
  )
