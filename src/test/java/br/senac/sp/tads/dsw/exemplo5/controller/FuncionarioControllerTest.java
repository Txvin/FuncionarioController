package br.senac.sp.tads.dsw.exemplo5.controller;


import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.senac.sp.tads.dsw.exemplo5.model.Departamento;
import br.senac.sp.tads.dsw.exemplo5.model.Funcionario;
import br.senac.sp.tads.dsw.exemplo5.repository.DepartamentoRepository;
import br.senac.sp.tads.dsw.exemplo5.repository.FuncionarioRepository;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional 
public class FuncionarioControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Departamento departamentoPadrao;

    @BeforeEach
    void setUp() {
        Departamento departamento = new Departamento();
        departamento.setNome("Desenvolvimento");
        departamento.setOrcamento(200000.00);
        departamentoPadrao = departamentoRepository.save(departamento);
    }

    // Método auxiliar: cria e salva um funcionário no banco para os testes que precisam de um registro existente
    private Funcionario salvarFuncionario(String nome, LocalDate dataContratacao, boolean trabalhoRemoto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(nome);
        funcionario.setDataContratacao(dataContratacao);
        funcionario.setTrabalhoRemoto(trabalhoRemoto);
        funcionario.setDepartamento(departamentoPadrao);
        return funcionarioRepository.save(funcionario);
    }

    @Test
    void deveCriarFuncionarioComSucesso() throws Exception {
        // 1 - Criar o objeto que queremos enviar
        Funcionario funcionario = new Funcionario();
        funcionario.setNome("Maria Silva");
        funcionario.setDataContratacao(LocalDate.of(2024, 1, 15));
        funcionario.setTrabalhoRemoto(true);
        funcionario.setDepartamento(departamentoPadrao);

        // Converter o objeto JAVA para uma String (texto JSON)
        String jsonRequisicao = objectMapper.writeValueAsString(funcionario);

        // 2 - Enviar o POST para a url da API
        mockMvc.perform(post("/api/funcionarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequisicao))
        // 3 - Verificar se a resposta é a esperada
                .andExpect(status().isCreated()) // HTTP 201
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.dataContratacao").value("2024-01-15"))
                .andExpect(jsonPath("$.trabalhoRemoto").value(true))
                .andExpect(jsonPath("$.departamento.id").value(departamentoPadrao.getId()));
    }

    @Test
    void deveRetornarErro400AoCriarFuncionarioComDataFutura() throws Exception {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome("João Futuro");
        funcionario.setDataContratacao(LocalDate.now().plusDays(10)); // data no futuro -> inválida (@PastOrPresent)
        funcionario.setTrabalhoRemoto(false);
        funcionario.setDepartamento(departamentoPadrao);

        String jsonRequisicao = objectMapper.writeValueAsString(funcionario);

        mockMvc.perform(post("/api/funcionarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequisicao))
                .andExpect(status().isBadRequest()); // HTTP 400
    }

    @Test
    void deveListarTodosOsFuncionarios() throws Exception {
        salvarFuncionario("Ana Souza", LocalDate.of(2023, 5, 10), false); // coloca 1 registro no banco de dados

        mockMvc.perform(get("/api/funcionarios"))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$[0].nome").value("Ana Souza")); // Posição zero do array
    }

    @Test
    void deveBuscarFuncionarioPorId() throws Exception {
        Funcionario funcionarioSalvo = salvarFuncionario("Carlos Lima", LocalDate.of(2022, 3, 20), true);

        mockMvc.perform(get("/api/funcionarios/" + funcionarioSalvo.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(funcionarioSalvo.getId()))
                .andExpect(jsonPath("$.nome").value("Carlos Lima"))
                .andExpect(jsonPath("$.trabalhoRemoto").value(true));
    }

    @Test
    void deveAtualizarFuncionario() throws Exception {
        // 1 - Prepara o registro antigo no banco de dados
        Funcionario funcionarioSalvo = salvarFuncionario("Paula Mendes", LocalDate.of(2021, 8, 1), false);

        // 2 - Cria os novos dados para o PUT
        Funcionario funcionarioAtualizado = new Funcionario();
        funcionarioAtualizado.setNome("Paula Mendes Santos");
        funcionarioAtualizado.setDataContratacao(LocalDate.of(2021, 8, 1));
        funcionarioAtualizado.setTrabalhoRemoto(true);
        funcionarioAtualizado.setDepartamento(departamentoPadrao);

        String jsonAtualizado = objectMapper.writeValueAsString(funcionarioAtualizado);

        // 3 - Executa e verifica se atualizou
        mockMvc.perform(put("/api/funcionarios/" + funcionarioSalvo.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonAtualizado))
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.id").value(funcionarioSalvo.getId()))
                .andExpect(jsonPath("$.nome").value("Paula Mendes Santos"))
                .andExpect(jsonPath("$.trabalhoRemoto").value(true));
    }

    @Test 
    void deveApagarFuncionario() throws Exception {
        Funcionario funcionarioSalvo = salvarFuncionario("Pedro Rocha", LocalDate.of(2020, 11, 30), false);

        // Deleta e espera 204 No Content
        mockMvc.perform(delete("/api/funcionarios/" + funcionarioSalvo.getId()))
                .andExpect(status().isNoContent());

        // Confirma que apagou tentando buscar (espera 404 Not Found)
        mockMvc.perform(get("/api/funcionarios/" + funcionarioSalvo.getId()))
                .andExpect(status().isNotFound());
    }
}