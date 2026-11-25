'use strict';

const categorias = { CLINICO_GERAL: 'Clínico geral', ESPECIALISTA: 'Especialista', CIRURGIAO: 'Cirurgião' };
const cadastro = document.querySelector('#formulario-profissional');
const filtro = document.querySelector('#formulario-filtro');
let selecionado = null;
let buscaAtual = 0;

function mensagem(texto, erro = false) {
    const elemento = document.querySelector('#mensagem');
    elemento.textContent = texto;
    elemento.setAttribute('role', erro ? 'alert' : 'status');
}

async function api(caminho = '', metodo = 'GET', dados) {
    const resposta = await fetch('/profissionais' + caminho, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: dados === undefined ? undefined : JSON.stringify(dados)
    });
    if (!resposta.ok) {
        const erro = await resposta.json().catch(() => ({}));
        throw new Error(erro.mensagem || 'Não foi possível concluir a operação.');
    }
    return resposta.status === 204 ? null : resposta.json();
}

function executar(acao) {
    return acao().catch(erro => mensagem(erro.message, true));
}

function elemento(tag, texto) {
    const novo = document.createElement(tag);
    novo.textContent = texto;
    return novo;
}

function ajustarCategoria() {
    const categoria = cadastro.elements.categoria.value;
    for (const [id, nome, mostrar] of [
        ['campo-especialidade', 'especialidade', categoria !== 'CLINICO_GERAL'],
        ['campo-area', 'areaCirurgica', categoria === 'CIRURGIAO']
    ]) {
        document.getElementById(id).hidden = !mostrar;
        cadastro.elements[nome].disabled = !mostrar;
        cadastro.elements[nome].required = mostrar;
    }
}

async function listar() {
    const busca = ++buscaAtual;
    const parametros = new URLSearchParams();
    for (const campo of ['nome', 'categoria', 'especialidade']) {
        if (filtro.elements[campo].value.trim()) parametros.set(campo, filtro.elements[campo].value.trim());
    }
    const profissionais = await api('?' + parametros);
    if (busca !== buscaAtual) return;
    if (filtro.elements.ordem.value === 'desc') profissionais.reverse();
    const lista = document.querySelector('#lista-profissionais');
    lista.replaceChildren();
    document.querySelector('#resumo').textContent = `${profissionais.length} profissional(is) encontrado(s)`;
    for (const profissional of profissionais) {
        const linha = document.createElement('tr');
        for (const texto of [profissional.nome, categorias[profissional.categoria], profissional.registroProfissional, profissional.especialidade || '—']) {
            linha.append(elemento('td', texto));
        }
        const acoes = document.createElement('td');
        for (const [rotulo, acao] of [
            ['Detalhes', () => detalhar(profissional.id)],
            ['Editar', () => editar(profissional)],
            ['Excluir', async () => {
                if (!window.confirm(`Excluir ${profissional.nome}?`)) return;
                await api('/' + profissional.id, 'DELETE');
                if (selecionado === profissional.id) {
                    selecionado = null;
                    document.querySelector('#detalhes').hidden = true;
                }
                if (cadastro.elements.id.value === String(profissional.id)) cadastro.reset();
                await listar();
                mensagem('Profissional excluído.');
            }]
        ]) {
            const botao = elemento('button', rotulo);
            botao.type = 'button';
            botao.addEventListener('click', () => executar(acao));
            acoes.append(botao, document.createTextNode(' '));
        }
        linha.append(acoes);
        lista.append(linha);
    }
    if (!profissionais.length) {
        const celula = elemento('td', 'Nenhum profissional encontrado.');
        celula.colSpan = 5;
        const linha = document.createElement('tr');
        linha.append(celula);
        lista.append(linha);
    }
}

function editar(profissional) {
    for (const campo of ['id', 'nome', 'registroProfissional', 'telefone', 'email', 'categoria', 'especialidade', 'areaCirurgica']) {
        cadastro.elements[campo].value = profissional[campo] ?? '';
    }
    ajustarCategoria();
    document.querySelector('#titulo-cadastro').textContent = 'Editar profissional';
    cadastro.elements.nome.focus();
}

function preencherLista(id, registros, formatar) {
    const lista = document.getElementById(id);
    lista.replaceChildren(...(registros.length ? registros.map(registro => elemento('li', formatar(registro))) : [elemento('li', 'Nenhum registro encontrado.')]));
}

async function detalhar(id) {
    selecionado = id;
    const [profissional, consultas, encaminhamentos, procedimentos] = await Promise.all([
        api('/' + id), api(`/${id}/consultas`), api(`/${id}/encaminhamentos`), api(`/${id}/procedimentos`)
    ]);
    if (selecionado !== id) return;
    document.querySelector('#titulo-detalhes').textContent = profissional.nome;
    const dados = document.querySelector('#dados-profissional');
    dados.replaceChildren();
    for (const [rotulo, valor] of [
        ['Categoria', categorias[profissional.categoria]], ['Registro', profissional.registroProfissional],
        ['Telefone', profissional.telefone], ['E-mail', profissional.email],
        ['Especialidade', profissional.especialidade], ['Atuação cirúrgica', profissional.areaCirurgica]
    ]) {
        if (valor) dados.append(elemento('dt', rotulo), elemento('dd', valor));
    }
    preencherLista('consultas-profissional', consultas, c => `${new Date(c.dataHora).toLocaleString('pt-BR')} — Paciente ${c.pacienteId}: ${c.motivo} (${c.situacao}). ${c.observacoes || ''}`);
    preencherLista('encaminhamentos-profissional', encaminhamentos, e => `Paciente ${e.pacienteId} — ${e.especialidade}: ${e.motivo}`);
    preencherLista('procedimentos-profissional', procedimentos, p => `${new Date(p.dataHora).toLocaleString('pt-BR')} — Paciente ${p.pacienteId}: ${p.descricao}`);
    document.querySelector('#secao-encaminhamentos').hidden = profissional.categoria !== 'CLINICO_GERAL';
    document.querySelector('#secao-procedimentos').hidden = profissional.categoria !== 'CIRURGIAO';
    document.querySelector('#detalhes').hidden = false;
}

cadastro.elements.categoria.addEventListener('change', ajustarCategoria);
document.querySelector('a[href="#cadastro"]').addEventListener('click', () => {
    cadastro.reset();
    cadastro.elements.nome.focus();
});
cadastro.addEventListener('reset', () => {
    setTimeout(() => {
        cadastro.elements.id.value = '';
        document.querySelector('#titulo-cadastro').textContent = 'Cadastrar profissional';
        ajustarCategoria();
    }, 0);
});
cadastro.addEventListener('submit', evento => {
    evento.preventDefault();
    executar(async () => {
        const dados = Object.fromEntries(new FormData(cadastro));
        const id = dados.id;
        delete dados.id;
        const profissional = await api(id ? '/' + id : '', id ? 'PUT' : 'POST', dados);
        cadastro.reset();
        await listar();
        await detalhar(profissional.id);
        mensagem('Profissional salvo.');
    });
});
filtro.addEventListener('submit', evento => { evento.preventDefault(); executar(listar); });
filtro.addEventListener('reset', () => setTimeout(() => executar(listar), 0));
filtro.elements.ordem.addEventListener('change', () => executar(listar));

for (const [id, caminho] of [['formulario-encaminhamento', 'encaminhamentos'], ['formulario-procedimento', 'procedimentos']]) {
    const formulario = document.getElementById(id);
    formulario.addEventListener('submit', evento => {
        evento.preventDefault();
        executar(async () => {
            const profissionalId = selecionado;
            const dados = Object.fromEntries(new FormData(formulario));
            dados.pacienteId = Number(dados.pacienteId);
            await api(`/${profissionalId}/${caminho}`, 'POST', dados);
            formulario.reset();
            if (selecionado === profissionalId) await detalhar(profissionalId);
            mensagem('Registro salvo.');
        });
    });
}

ajustarCategoria();
executar(listar);
