package com.naal.bankmind.service.Default;

import com.naal.bankmind.dto.Default.Request.BatchFilterRequestDTO;
import com.naal.bankmind.dto.Default.Response.AccountSummaryDTO;
import com.naal.bankmind.dto.Default.Response.CustomerSearchResponseDTO;
import com.naal.bankmind.entity.AccountDetails;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.repository.Default.AccountDetailsRepository;
import com.naal.bankmind.repository.Default.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para operaciones relacionadas con clientes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountDetailsRepository accountDetailsRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Busca clientes por nombre o ID.
     */
    public List<CustomerSearchResponseDTO> searchCustomers(String searchTerm) {
        log.info("Buscando clientes con término: {}", searchTerm);

        List<Customer> customers = customerRepository.searchByNameOrId(searchTerm);

        return customers.stream()
                .limit(10) // Limitar resultados
                .map(this::mapToSearchResponse)
                .collect(Collectors.toList());
    }

    /**
     * Filtra clientes con parámetros y retorna lista de recordIds de sus cuentas.
     */
    public List<Long> filterCustomersGetRecordIds(BatchFilterRequestDTO filters) {
        log.info("Filtrando clientes para predicción batch");

        // Parsear filtros
        Integer edadMin = filters.getEdadMin();
        Integer edadMax = filters.getEdadMax();
        Integer educacion = parseEducacionToId(filters.getEducacion());
        Integer estadoCivil = parseEstadoCivilToId(filters.getEstadoCivil());
        LocalDateTime fechaDesde = parseDate(filters.getFechaDesde());
        LocalDateTime fechaHasta = parseDate(filters.getFechaHasta());

        // Buscar clientes filtrados
        List<Customer> customers = customerRepository.findByFilters(
                edadMin, edadMax, educacion, estadoCivil, fechaDesde, fechaHasta);

        log.info("Encontrados {} clientes con filtros aplicados", customers.size());

        // Obtener todos los recordIds de las cuentas de estos clientes
        List<Long> recordIds = new ArrayList<>();
        for (Customer customer : customers) {
            List<AccountDetails> accounts = accountDetailsRepository
                    .findByCustomer_IdCustomer(customer.getIdCustomer());
            for (AccountDetails account : accounts) {
                recordIds.add(account.getRecordId());
            }
        }

        log.info("Total de cuentas (recordIds): {}", recordIds.size());
        return recordIds;
    }

    /**
     * Obtiene las cuentas de un cliente específico.
     */
    public List<AccountSummaryDTO> getAccountsByCustomerId(Long customerId) {
        log.info("Obteniendo cuentas del cliente: {}", customerId);

        List<AccountDetails> accounts = accountDetailsRepository.findByCustomer_IdCustomer(customerId);

        return accounts.stream()
                .map(this::mapToAccountSummary)
                .collect(Collectors.toList());
    }

    private CustomerSearchResponseDTO mapToSearchResponse(Customer customer) {
        List<AccountDetails> accounts = accountDetailsRepository.findByCustomer_IdCustomer(customer.getIdCustomer());
        List<AccountSummaryDTO> accountDTOs = accounts.stream()
                .map(this::mapToAccountSummary)
                .collect(Collectors.toList());

        String nombre = (customer.getFirstName() != null ? customer.getFirstName() : "") + " " +
                (customer.getSurname() != null ? customer.getSurname() : "");

        String educacion = customer.getEducation() != null ? mapEducation(customer.getEducation().getIdEducation())
                : "No especificado";

        String estadoCivil = customer.getMarriage() != null ? mapMarriage(customer.getMarriage().getIdMarriage())
                : "No especificado";

        String fechaRegistro = customer.getIdRegistrationDate() != null
                ? customer.getIdRegistrationDate().format(DATE_FORMATTER)
                : "";

        return new CustomerSearchResponseDTO(
                customer.getIdCustomer(),
                nombre.trim(),
                customer.getAge(),
                educacion,
                estadoCivil,
                fechaRegistro,
                accountDTOs);
    }

    private AccountSummaryDTO mapToAccountSummary(AccountDetails account) {
        return new AccountSummaryDTO(
                account.getRecordId(),
                account.getLimitBal(),
                account.getBalance(),
                account.getTenure(),
                account.getIsActiveMember());
    }

    private String mapEducation(Integer idEducation) {
        return switch (idEducation) {
            case 1 -> "Postgrado";
            case 2 -> "Universitaria";
            case 3 -> "Secundaria";
            case 4 -> "Primaria";
            default -> "Otro";
        };
    }

    private String mapMarriage(Integer idMarriage) {
        return switch (idMarriage) {
            case 1 -> "Casado";
            case 2 -> "Soltero";
            case 3 -> "Divorciado";
            default -> "Otro";
        };
    }

    private Integer parseEducacionToId(String educacion) {
        if (educacion == null || educacion.equalsIgnoreCase("all"))
            return null;
        return switch (educacion) {
            case "Postgrado" -> 1;
            case "Universitaria" -> 2;
            case "Secundaria" -> 3;
            case "Primaria" -> 4;
            default -> null;
        };
    }

    private Integer parseEstadoCivilToId(String estadoCivil) {
        if (estadoCivil == null || estadoCivil.equalsIgnoreCase("all"))
            return null;
        return switch (estadoCivil) {
            case "Casado" -> 1;
            case "Soltero" -> 2;
            case "Divorciado" -> 3;
            default -> null;
        };
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty())
            return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER).atStartOfDay();
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateStr);
            return null;
        }
    }
}
