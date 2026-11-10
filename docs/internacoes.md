# Internações (Sprint 2)

Responsável: João Paulo

Segue o mesmo padrão de Quartos: dados em memória, IDs `long`, API REST em `/internacoes` e tela Thymeleaf em `/internacoes.html`.
A ocupação dos quartos é sempre feita pelo `QuartoService` (`registrarEntrada` e `registrarAlta`), então a regra de vagas fica num lugar só.

## Regras de negócio

1. Toda internação precisa de paciente, profissional responsável, quarto, data de entrada e data prevista de alta.
2. A data de entrada não pode ser futura.
3. A data prevista de alta não pode ser anterior à data de entrada.
4. O quarto precisa ter vaga.
5. Um paciente não pode ter duas internações ativas ao mesmo tempo.
6. Só internação `ATIVA` pode ser editada, transferida ou receber alta.
7. A alta não pode ser antes da entrada nem futura. Depois da alta a internação fica `FINALIZADA` e não muda mais (`SituacaoInternacao.permiteTransicaoPara`).
8. Na transferência, o quarto de destino precisa ser outro e ter vaga. A vaga do quarto antigo é liberada.
9. Se qualquer regra falhar, nada é gravado e a ocupação dos quartos não muda.

## Endpoints

| Método | Caminho | Corpo (JSON) |
|---|---|---|
| GET | `/internacoes` | filtros opcionais na URL: `pacienteId`, `profissionalId`, `quartoId`, `situacao` |
| GET | `/internacoes/{id}` | |
| POST | `/internacoes` | `pacienteId`, `profissionalId`, `quartoId`, `dataEntrada`, `dataPrevistaAlta`, `observacoes` |
| PUT | `/internacoes/{id}` | `profissionalId`, `dataPrevistaAlta`, `observacoes` |
| PATCH | `/internacoes/{id}/transferencia` | `quartoId` |
| PATCH | `/internacoes/{id}/alta` | `dataEfetivaAlta`, `observacoes` |

Erros (`{"mensagem": "..."}`): 400 dados inválidos, 404 não encontrado, 409 conflito com regra (quarto lotado, internação finalizada etc.).
Datas no formato `AAAA-MM-DD`. Não há DELETE: internação faz parte do histórico do paciente.

## Pendências com o grupo

- Paciente e profissional ainda são informados só pelo ID, igual em Consultas. Quando os cadastros existirem, dá para validar se o ID existe e mostrar o nome.

## Trecho para o diagrama de classes (PlantUML)

```plantuml
class Internacao {
  -id: long
  -pacienteId: long
  -profissionalId: long
  -quartoId: long
  -dataEntrada: LocalDate
  -dataPrevistaAlta: LocalDate
  -dataEfetivaAlta: LocalDate
  -observacoes: String
  -situacao: SituacaoInternacao
  +atualizar(profissionalId, dataPrevistaAlta, observacoes): void
  +transferirPara(novoQuartoId): void
  +darAlta(dataAlta, observacao): void
  +estaAtiva(): boolean
  +copia(): Internacao
}

enum SituacaoInternacao {
  ATIVA
  FINALIZADA
  +permiteTransicaoPara(destino): boolean
}

class InternacaoService {
  +registrar(...): Internacao
  +listar(pacienteId, profissionalId, quartoId, situacao): List<Internacao>
  +buscar(id): Internacao
  +atualizar(...): Internacao
  +transferir(id, novoQuartoId): Internacao
  +editar(...): Internacao
  +darAlta(id, dataAlta, observacoes): Internacao
}

interface InternacaoRepository
class InternacaoRepositoryEmMemoria

Internacao --> SituacaoInternacao
InternacaoService --> InternacaoRepository
InternacaoRepositoryEmMemoria ..|> InternacaoRepository
InternacaoService --> QuartoService : registrarEntrada / registrarAlta
```
