package br.com.fiap.controller;

import br.com.fiap.dto.UsuarioCreateDTO;
import br.com.fiap.dto.UsuarioDTO;
import br.com.fiap.dto.UsuarioMapper;
import br.com.fiap.model.Empresa;
import br.com.fiap.model.Usuario;
import br.com.fiap.repository.EmpresaRepository;
import br.com.fiap.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MessageSource messageSource;

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> create(@Valid @RequestBody UsuarioCreateDTO dto) {
        Usuario toSave = UsuarioMapper.fromCreateDTO(dto);
        if (dto.getIdEmpresa() != null) {
            Empresa e = empresaRepository.findById(dto.getIdEmpresa()).orElse(null);
            toSave.setIdEmpresa(e);
        }
        // Encode password before saving
        toSave.setDsSenha(passwordEncoder.encode(toSave.getDsSenha()));

            
            Usuario saved = usuarioService.create(toSave);
            return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioMapper.toDTO(saved));
    }

    // Form submission endpoint for Thymeleaf UI
    @PostMapping(path = "/form", consumes = {"application/x-www-form-urlencoded"})
    public String createFromForm(@Valid UsuarioCreateDTO dto, org.springframework.validation.BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            java.util.Map<String,String> errors = new java.util.HashMap<>();
            bindingResult.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("editUsuario", dto);
            return "redirect:/usuarios";
        }
        Usuario toSave = UsuarioMapper.fromCreateDTO(dto);
        if (dto.getIdEmpresa() != null) {
            Empresa e = empresaRepository.findById(dto.getIdEmpresa()).orElse(null);
            toSave.setIdEmpresa(e);
        }
        toSave.setDsSenha(passwordEncoder.encode(toSave.getDsSenha()));
        try {
            usuarioService.create(toSave);
            String msg = messageSource.getMessage("usuario.created", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("message", msg);
        } catch (DataIntegrityViolationException ex) {
            String err = messageSource.getMessage("user.email.exists", null, LocaleContextHolder.getLocale());
            redirectAttributes.addFlashAttribute("error", err);
        }
        return "redirect:/usuarios";
    }

    // Update via form
    @PostMapping(path = "/form/{id}", consumes = {"application/x-www-form-urlencoded"})
    public String updateFromForm(@PathVariable Long id, @Valid UsuarioCreateDTO dto, org.springframework.validation.BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            java.util.Map<String,String> errors = new java.util.HashMap<>();
            bindingResult.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("editUsuario", dto);
            return "redirect:/usuarios?editId=" + id;
        }
        usuarioService.findById(id).ifPresent(existing -> {
            existing.setNmUsuario(dto.getNmUsuario());
            existing.setDsEmail(dto.getDsEmail());
            existing.setTpUsuario(dto.getTpUsuario());
            if (dto.getIdEmpresa() != null) {
                Empresa e = empresaRepository.findById(dto.getIdEmpresa()).orElse(null);
                existing.setIdEmpresa(e);
            }
            // If password provided and not blank, update it
            if (dto.getDsSenha() != null && !dto.getDsSenha().isBlank()) {
                existing.setDsSenha(passwordEncoder.encode(dto.getDsSenha()));
            }
            usuarioService.update(existing);
        });
        String msg = messageSource.getMessage("usuario.updated", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/usuarios";
    }

    // Support PUT from HTML form via HiddenHttpMethodFilter
    @PutMapping(path = "/form/{id}", consumes = {"application/x-www-form-urlencoded"})
    public String updateFromFormPut(@PathVariable Long id, @Valid UsuarioCreateDTO dto, org.springframework.validation.BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        return updateFromForm(id, dto, bindingResult, redirectAttributes);
    }

    @PostMapping(path = "/delete/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public String deleteFromForm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioService.delete(id);
        String msg = messageSource.getMessage("usuario.deleted", null, LocaleContextHolder.getLocale());
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/usuarios";
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<Page<UsuarioDTO>> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        Pageable p = PageRequest.of(page, size);
        Page<UsuarioDTO> users = usuarioService.listAll(p).map(UsuarioMapper::toDTO);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<UsuarioDTO> get(@PathVariable Long id) {
        return usuarioService.findById(id)
                .map(UsuarioMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
