package com.library.service;

import com.library.model.ContactMessage;
import com.library.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    public ContactMessage save(ContactMessage message) {
        return contactMessageRepository.save(message);
    }

    public List<ContactMessage> getAll() {
        return contactMessageRepository.findAllByOrderBySubmittedAtDesc();
    }

    public void markResolved(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + id));
        message.setResolved(true);
        contactMessageRepository.save(message);
    }
}
