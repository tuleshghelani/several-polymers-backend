package com.inventory.service;

import com.inventory.entity.Client;
import com.inventory.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class SalesBillNumberGeneratorService {
    private final ClientRepository clientRepository;

    @Transactional
    public synchronized String generateInvoiceNumber(Client client) {
        Client lockedClient = clientRepository.findByIdWithPessimisticLock(client.getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));

        Long nextNumber = (lockedClient.getLastSalesBillNumber() == null ||
                lockedClient.getLastSalesBillNumber() < 1) ?
                1L : lockedClient.getLastSalesBillNumber() + 1;

        lockedClient.setLastSalesBillNumber(nextNumber);
        clientRepository.save(lockedClient);

        return String.format("WB-%d-%d", Year.now().getValue(), nextNumber);
    }
}


