package com.example.demo.note.controller;

import com.example.demo.auth.config.Constant;
import com.example.demo.note.model.AccessType;
import com.example.demo.note.model.Note;
import com.example.demo.note.service.NoteService;
import com.example.demo.user.User;
import com.example.demo.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private static final String REDIRECT_NOTE_ERROR = "redirect:error";
    private static final String REDIRECT_NOTE_LIST = "redirect:/note/list";
    private static final String REDIRECT_NOTE_DENIED = "redirect:/note/denied";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String NOTE_ERROR_ATTRIBUTE = "error";

    private final NoteService noteService;
    private final UserService userService;

    @GetMapping("/list")
    public String getNoteList(@RequestParam(value = "title", required = false) String title,
                              Model model) {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            return REDIRECT_LOGIN;
        }
        List<Note> userNotes;
        if (title != null) {
            userNotes = noteService.listNoteByContent(currentUser.getId(), title);
            model.addAttribute("notes", userNotes);
        } else {
            userNotes = noteService.findByUser(currentUser);
            model.addAttribute("notes", userNotes);
        }
        return "list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        Note note = new Note();
        model.addAttribute("note", note);
        return "create";
    }

    @PostMapping("/create")
    public String createNote(@ModelAttribute Note note, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user == null) {
            return REDIRECT_LOGIN;
        }
        note.setUser(user);

        if (note.getColor().equals(Constant.WHITE_COLOR)) {
            note.setColor(null);
        }

        if (saveNote(note, redirectAttributes)) {
            return REDIRECT_NOTE_ERROR;
        }

        return REDIRECT_NOTE_LIST;
    }

    @PostMapping("/delete")
    public String noteDelete(@RequestParam String id, HttpServletResponse response) throws IOException {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return REDIRECT_LOGIN;
        }

        Optional<Note> optionalNote = noteService.getById(id);
        if (optionalNote.isPresent()) {
            Note note = optionalNote.get();
            if (!note.getUser().equals(currentUser)) {
                return REDIRECT_NOTE_DENIED;
            }
            noteService.deleteById(id);
        }

        return REDIRECT_NOTE_LIST;
    }

    @GetMapping("/edit")
    public String noteEdit(@RequestParam String id, Model model) {
        User currentUser = userService.getCurrentUser();

        if (currentUser == null) {
            return REDIRECT_LOGIN;
        }

        Optional<Note> optionalNote = noteService.getById(id);

        if (optionalNote.isPresent()) {
            Note note = optionalNote.get();

            if (!note.getUser().equals(currentUser)) {
                return REDIRECT_NOTE_DENIED;
            }

            if (note.getColor() == null) {
                note.setColor(Constant.WHITE_COLOR);
            }

            model.addAttribute("note", note);
            return "edit";
        } else {

            return REDIRECT_NOTE_LIST;
        }
    }

    @PostMapping("/edit")
    public String noteSave(@ModelAttribute Note note, RedirectAttributes redirectAttributes) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return REDIRECT_LOGIN;
        }

        if (note.getColor().equals(Constant.WHITE_COLOR)) {
            note.setColor(null);
        }

        Optional<Note> existingNoteOptional = noteService.getById(note.getId());
        if (existingNoteOptional.isPresent()) {
            Note existingNote = existingNoteOptional.get();
            existingNote.setTitle(note.getTitle());
            existingNote.setContent(note.getContent());
            existingNote.setAccessType(note.getAccessType());
            existingNote.setColor(note.getColor());
            if (saveNote(existingNote, redirectAttributes)) {
                return REDIRECT_NOTE_ERROR;
            }
        } else {
            return REDIRECT_NOTE_LIST;
        }
        return REDIRECT_NOTE_LIST;
    }

    @GetMapping("/share/{id}")
    public String viewSharedNote(@PathVariable("id") String noteId, Model model) {
        Optional<Note> optionalNote = noteService.getById(noteId);
        Note note = new Note();

        if (optionalNote.isPresent()) {
            note = optionalNote.get();
        }
        if (note.getAccessType().equals(AccessType.PRIVATE)) {
            return REDIRECT_NOTE_DENIED;
        }

        model.addAttribute("note", note);
        return "access-permit";
    }

    @GetMapping("/error")
    public String error(Model model, @ModelAttribute(NOTE_ERROR_ATTRIBUTE) String errorMessage) {
        model.addAttribute("error", errorMessage);
        return "error";
    }

    @GetMapping("/denied")
    public String getDenied(Model model) {
        return "access-denied";
    }

    @GetMapping("/view/{id}")
    public String viewNote(@PathVariable("id") String noteId, Model model) {
        User currentUser = userService.getCurrentUser();
        Optional<Note> optionalNote = noteService.getById(noteId);

        if (optionalNote.isPresent()) {
            Note note = optionalNote.get();
            if (note.getUser().equals(currentUser) || note.getAccessType() == AccessType.PUBLIC) {
                model.addAttribute("note", note);
                return "view";
            } else {
                return REDIRECT_NOTE_DENIED;
            }
        }
        return REDIRECT_NOTE_LIST;
    }

    private boolean saveNote(Note note, RedirectAttributes redirectAttributes) {
        try {
            noteService.save(note);
            return false;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addAttribute(NOTE_ERROR_ATTRIBUTE, e.getMessage());
            return true;
        }
    }
}