package br.com.dextra.dextranet.conteudo.post;

import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

import br.com.dextra.dextranet.conteudo.post.comentario.Comentario;
import br.com.dextra.dextranet.conteudo.post.comentario.ComentarioRepository;
import br.com.dextra.dextranet.persistencia.EntidadeOrdenacao;
import br.com.dextra.teste.TesteIntegracaoBase;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Query.SortDirection;

public class PostRepositoryTest extends TesteIntegracaoBase {

	private PostRepository repositorio = new PostRepository();

	@After
	public void removePostsInseridos() {
		this.limpaPostsInseridos(repositorio);
	}

	@Test
	public void testaRemocao() {
		Post novoPost = new Post("usuario", "titulo", "conteudo");

		Post postCriado = repositorio.persiste(novoPost);
		postCriado.curtir("dextranet");

		String idDoPostCriado = postCriado.getId();
		repositorio.remove(idDoPostCriado);

		try {
			repositorio.obtemPorId(idDoPostCriado);
			Assert.fail();
		} catch (EntityNotFoundException e) {
			Assert.assertTrue(true);
		}
	}

	@Test
	public void testaListarPaginado() {
		int registrosPorPagina = 3;
		int pagina1 = 1;
		int pagina2 = 2;

		EntidadeOrdenacao dataAtualizacaoDescentente = new EntidadeOrdenacao(PostFields.dataDeAtualizacao.name(), SortDirection.DESCENDING);

		Post post01 = new Post("dextranet", "titulo 01", "conteudo 01");
		repositorio.persiste(post01);

		Post post02 = new Post("usuario", "titulo 02", "conteudo 02");
		repositorio.persiste(post02);

		Post post03 = new Post("dextranet", "titulo 03", "conteudo 03");
		repositorio.persiste(post03);

		Post post04 = new Post("outro-usuario", "titulo 04", "conteudo 04");
		repositorio.persiste(post04);

		Post post05 = new Post("usuario", "titulo 05", "conteudo 05");
		repositorio.persiste(post05);

		Post post06 = new Post("usuario", "titulo 06", "conteudo 06");
		repositorio.persiste(post06);

		List<Post> postsPagina01 = repositorio.lista(registrosPorPagina, pagina1, dataAtualizacaoDescentente);
		Assert.assertEquals(3, postsPagina01.size());
		Assert.assertEquals(post06, postsPagina01.get(0));
		Assert.assertEquals(post05, postsPagina01.get(1));
		Assert.assertEquals(post04, postsPagina01.get(2));

		List<Post> postsPagina02 = repositorio.lista(registrosPorPagina, pagina2, dataAtualizacaoDescentente);
		Assert.assertEquals(3, postsPagina02.size());
		Assert.assertEquals(post03, postsPagina02.get(0));
		Assert.assertEquals(post02, postsPagina02.get(1));
		Assert.assertEquals(post01, postsPagina02.get(2));
	}

	@Test
	public void testaRemocaoComentariosPorPost() {
		Post post01 = new Post("dextranet", "titulo 01", "conteudo 01");
		repositorio.persiste(post01);

		Comentario c = new Comentario(post01.getId(), "username", "conteudo");
		ComentarioRepository repositorioDeComentarios = new ComentarioRepository();
		repositorioDeComentarios.persiste(c);
		repositorio.remove(post01.getId());

		Assert.assertEquals(0, repositorioDeComentarios.listaPorPost(post01.getId()).size());

	}

}