package br.com.dextra.dextranet.conteudo.post;

import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import br.com.dextra.dextranet.conteudo.post.comentario.Comentario;
import br.com.dextra.dextranet.conteudo.post.comentario.ComentarioRepository;
import br.com.dextra.dextranet.conteudo.post.curtida.Curtida;
import br.com.dextra.dextranet.conteudo.post.curtida.CurtidaRepository;
import br.com.dextra.dextranet.persistencia.EntidadeBusca;
import br.com.dextra.dextranet.persistencia.EntidadeOrdenacao;
import br.com.dextra.dextranet.rest.config.Application;
import br.com.dextra.dextranet.seguranca.AutenticacaoService;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

@Path("/post")
public class PostRS {
	private PostRepository repositorioDePosts = new PostRepository();
	private CurtidaRepository repositorioDeCurtidas = new CurtidaRepository();
	private ComentarioRepository repositorioDeComentarios = new ComentarioRepository();

	@Path("/")
	@POST
	@Produces(Application.JSON_UTF8)
	public Response inserir(@FormParam("titulo") String titulo, @FormParam("conteudo") String conteudo) {
		Post  post = new Post(obtemUsuarioLogado(), titulo, conteudo);
		repositorioDePosts.persiste(post);
		return Response.ok().entity(post).build();
	}

	@Path("/{id}")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response obter(@PathParam("id") String id) throws EntityNotFoundException {
		Post post = repositorioDePosts.obtemPorId(id);
		return Response.ok().entity(post).build();
	}

	@Path("/{id}")
	@DELETE
	@Produces(Application.JSON_UTF8)
	public Response deletar(@PathParam("id") String id) throws EntityNotFoundException {
		String usuarioLogado = obtemUsuarioLogado();
		Post post = repositorioDePosts.obtemPorId(id);
		if (post.getUsuario().equals(usuarioLogado)) {
			repositorioDePosts.remove(id);
			return Response.ok().build();
		} else {
			return Response.status(Status.FORBIDDEN).build();
		}

	}

	@Path("/")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response listar(@QueryParam("r") @DefaultValue(Application.REGISTROS_POR_PAGINA) Integer registrosPorPagina, @QueryParam("p") @DefaultValue("1") Integer pagina) {
		List<Post> posts = this.listarPostsOrdenados(registrosPorPagina, pagina);
		return Response.ok().entity(posts).build();
	}

	@Path("/{postId}/curtida")
	@POST
	@Produces(Application.JSON_UTF8)
	public Response curtir(@PathParam("postId") String postId) throws EntityNotFoundException {
		Post post = repositorioDePosts.obtemPorId(postId);
		Curtida curtida = post.curtir(this.obtemUsuarioLogado());

		if (curtida != null) {
			repositorioDeCurtidas.persiste(curtida);
		}

		repositorioDePosts.persiste(post);
		List<Curtida> curtidas = repositorioDeCurtidas.listaPorConteudo(postId);
		return Response.ok().entity(curtidas.size()).build();
	}

	@Path("/{postId}/curtida")
	@DELETE
	@Produces(Application.JSON_UTF8)
	public Response descurtir(@PathParam("postId") String postId) throws EntityNotFoundException {
		Post post = repositorioDePosts.obtemPorId(postId);
		String usuarioLogado = obtemUsuarioLogado();

		post.descurtir(usuarioLogado);
		repositorioDeCurtidas.remove(post.getId(), usuarioLogado);
		repositorioDePosts.persiste(post);

		return Response.ok().entity(post).build();
	}

	@Path("/{postId}/curtida")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response listarCurtidas(@PathParam("postId") String postId) throws EntityNotFoundException {
		List<Curtida> curtidas = repositorioDeCurtidas.listaPorConteudo(postId);
		return Response.ok().entity(curtidas).build();
	}

	protected List<Post> listarPostsOrdenados(Integer registrosPorPagina, Integer pagina) {
		EntidadeOrdenacao dataDeAtualizacaoDecrescente = new EntidadeOrdenacao(PostFields.dataDeAtualizacao.name(), SortDirection.DESCENDING);

		List<Post> posts = repositorioDePosts.lista(registrosPorPagina, pagina, dataDeAtualizacaoDecrescente);
		return posts;
	}

	@Path("/{postId}/comentario")
	@POST
	@Produces(Application.JSON_UTF8)
	public Response comentar(@PathParam("postId") String postId, @FormParam("conteudo") String conteudo) throws EntityNotFoundException {
		Post post = repositorioDePosts.obtemPorId(postId);
		Comentario comentario = post.comentar(this.obtemUsuarioLogado(), conteudo);
		repositorioDePosts.persiste(post);
		repositorioDeComentarios.persiste(comentario);
		return Response.ok().entity(comentario).build();
	}

	@Path("/{postId}/comentario")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response listarComentarios(@PathParam("postId") String postId) throws EntityNotFoundException {
		List<Comentario> comentarios = repositorioDeComentarios.listaPorPost(postId);
		return Response.ok().entity(comentarios).build();
	}

	@Path("/{postId}/{comentarioId}/curtida")
	@POST
	@Produces(Application.JSON_UTF8)
	public Response curtirComentario(@PathParam("postId") String postId, @PathParam("comentarioId") String comentarioId) throws EntityNotFoundException {
		Comentario comentario = repositorioDeComentarios.obtemPorId(comentarioId);
		Curtida curtida = comentario.curtir(this.obtemUsuarioLogado());

		if (curtida != null) {
			repositorioDeCurtidas.persiste(curtida);
		}

		repositorioDeComentarios.persiste(comentario);

		return Response.ok().entity(comentario).build();
	}

	@Path("/{postId}/{comentarioId}/curtida")
	@DELETE
	@Produces(Application.JSON_UTF8)
	public Response descurtirComentario(@PathParam("postId") String postId, @PathParam("comentarioId") String comentarioId) throws EntityNotFoundException {
		Comentario comentario = repositorioDeComentarios.obtemPorId(comentarioId);
		comentario.descurtir(this.obtemUsuarioLogado());

		repositorioDeCurtidas.remove(comentarioId, obtemUsuarioLogado());
		repositorioDeComentarios.persiste(comentario);

		return Response.ok().entity(comentario).build();
	}

	@Path("/{postId}/{comentarioId}/curtida")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response listarCurtidasComentario(@PathParam("postId") String postId, @PathParam("comentarioId") String comentarioId) throws EntityNotFoundException {
		List<Curtida> curtidas = repositorioDeCurtidas.listaPorConteudo(comentarioId);
		return Response.ok().entity(curtidas).build();
	}

	protected String obtemUsuarioLogado() {
		return AutenticacaoService.identificacaoDoUsuarioLogado();
	}

	@Path("/{comentarioId}/comentario")
	@DELETE
	@Produces(Application.JSON_UTF8)
	public Response deletarComentario(@PathParam("comentarioId") String comentarioId) throws EntityNotFoundException {

		String usuarioLogado = obtemUsuarioLogado();
		Comentario comentario = repositorioDeComentarios.obtemPorId(comentarioId);
		if (comentario.getUsuario().equals(usuarioLogado)) {
			repositorioDeComentarios.remove(comentarioId);
			return Response.ok().build();
		} else {
			return Response.status(Status.FORBIDDEN).build();
		}

	}

	@Path("/count/")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response verificaNovosPosts(@QueryParam("d") Date data) {
		PostRepository postRepository = new PostRepository();
		EntidadeBusca entidadeBusca = new EntidadeBusca();
		entidadeBusca.setData(data);
		entidadeBusca.setCampo(PostFields.dataDeAtualizacao.name());
		entidadeBusca.setClazz(Post.class.getName());
		entidadeBusca.setDirecaoOrdenacao(SortDirection.DESCENDING);
		entidadeBusca.setFiltro(FilterOperator.GREATER_THAN);
		Iterable<Entity> total = postRepository.paginar(entidadeBusca);
		List<Post> novosPosts = postRepository.toPosts(total);

		return Response.ok().entity(novosPosts).build();
	}

	@Path("/paginar/")
	@GET
	@Produces(Application.JSON_UTF8)
	public Response paginar(@QueryParam("u") Date data) {
		PostRepository postRepository = new PostRepository();
		EntidadeBusca entidadeBusca = new EntidadeBusca();
		entidadeBusca.setData(data);
		entidadeBusca.setCampo(PostFields.dataDeAtualizacao.name());
		entidadeBusca.setClazz(Post.class.getName());
		entidadeBusca.setDirecaoOrdenacao(SortDirection.DESCENDING);
		entidadeBusca.setLimite(Integer.parseInt(Application.REGISTROS_POR_PAGINA));
		entidadeBusca.setFiltro(FilterOperator.LESS_THAN);
		Iterable<Entity> total = postRepository.paginar(entidadeBusca);
		List<Post> novosPosts = postRepository.toPosts(total);

		return Response.ok().entity(novosPosts).build();
	}
}