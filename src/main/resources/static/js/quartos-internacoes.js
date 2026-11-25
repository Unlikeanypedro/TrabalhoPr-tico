const painel = document.getElementById('resultado-rest');
const chaveMensagem = 'mensagem-' + painel.dataset.voltar;

function mostrarMensagem(mensagem, erro) {
    painel.textContent = mensagem;
    painel.setAttribute('role', erro ? 'alert' : 'status');
    painel.hidden = false;
    if (erro) {
        painel.focus();
    }
}

try {
    const mensagem = sessionStorage.getItem(chaveMensagem);
    if (mensagem) {
        sessionStorage.removeItem(chaveMensagem);
        mostrarMensagem(mensagem, false);
    }
} catch {}

document.addEventListener('submit', async evento => {
    const formulario = evento.target;
    if (!formulario.dataset.api || evento.defaultPrevented) {
        return;
    }
    evento.preventDefault();

    const botao = evento.submitter;
    if (botao) {
        botao.disabled = true;
    }

    const dados = new FormData(formulario);
    const metodo = formulario.dataset.metodo;
    let url = formulario.dataset.api;
    if (formulario.id === 'formulario-alta') {
        const id = encodeURIComponent(dados.get('internacaoId'));
        url += '/' + id + '/alta';
    }

    const corpo = {};
    for (const campo of (formulario.dataset.campos || '').split(' ').filter(Boolean)) {
        const valor = dados.get(campo);
        const elemento = formulario.elements.namedItem(campo);
        if (valor === '' || valor == null) {
            corpo[campo] = null;
        } else if (elemento.type === 'number' && !campo.endsWith('Id')) {
            corpo[campo] = Number(valor);
        } else {
            corpo[campo] = valor;
        }
    }

    try {
        const resposta = await fetch(url, {
            method: metodo,
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: metodo === 'DELETE' ? undefined : JSON.stringify(corpo)
        });
        if (!resposta.ok) {
            const falha = await resposta.json().catch(() => ({}));
            mostrarMensagem(falha.mensagem || 'O servidor não conseguiu concluir a operação.', true);
            return;
        }
        try {
            sessionStorage.setItem(chaveMensagem, formulario.dataset.sucesso);
        } catch {}
        window.location.assign(painel.dataset.voltar);
    } catch (erro) {
        mostrarMensagem('Não foi possível concluir a operação. Confira a conexão com o servidor.', true);
    } finally {
        if (botao) {
            botao.disabled = false;
        }
    }
});
