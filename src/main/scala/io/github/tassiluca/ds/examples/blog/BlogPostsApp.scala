package io.github.tassiluca.ds.examples.blog

trait BlogPostsApp extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  val contentVerifier: ContentVerifier
  val authorsVerifier: AuthorsVerifier

  override val repository: PostsRepository = PostsRepository()
