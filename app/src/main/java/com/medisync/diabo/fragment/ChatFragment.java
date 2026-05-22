package com.medisync.diabo.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.medisync.diabo.adapter.ChatAdapter;
import com.medisync.diabo.databinding.FragmentChatBinding;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.MedicalReport;
import com.medisync.diabo.service.ChatService;
import com.medisync.diabo.service.OCRService;
import com.medisync.diabo.service.OllamaRouter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private final List<ChatAdapter.Message> messages = new ArrayList<>();
    private ChatService chatService;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isWaitingForResponse = false;
    private final ActivityResultLauncher<String> attachLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleAttachment
    );
    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        binding.etMessage.setText(matches.get(0));
                        binding.etMessage.setSelection(binding.etMessage.length());
                    }
                }
            }
    );
    private final ActivityResultLauncher<String> audioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) startSpeechInput();
                else Toast.makeText(requireContext(), "Microphone permission is required for voice input.", Toast.LENGTH_SHORT).show();
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatService = new ChatService();
        loadChatHistory();
        adapter = new ChatAdapter(messages);
        binding.rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChat.setAdapter(adapter);
        if (messages.isEmpty()) {
            addMessage("Medical safety note: I can explain reports and general diabetes care, but I am not a replacement for your doctor or emergency care.", false);
        }

        binding.btnAttach.setOnClickListener(v -> attachLauncher.launch("*/*"));
        binding.btnMic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startSpeechInput();
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        // Explain Lab Results – fetch latest report and build prompt
        binding.chipExplainLab.setOnClickListener(v -> {
            addMessage("Explain my lab results", true);
            hideTipCard();
            addMessage("Fetching your latest report...", false);
            dbExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                MedicalReport report = db.appDao().getLatestReportSync();
                mainHandler.post(() -> {
                    // Remove placeholder
                    if (!messages.isEmpty()) {
                        messages.remove(messages.size() - 1);
                        adapter.notifyItemRemoved(messages.size());
                    }
                    if (report == null || (report.extractedText == null && report.aiInsights == null)) {
                        addMessage("No lab report found yet. Please upload a report first.", false);
                        return;
                    }
                    StringBuilder context = new StringBuilder();
                    if (report.title != null && !report.title.isEmpty()) {
                        context.append("Report Title: ").append(report.title).append("\n");
                    }
                    if (report.extractedText != null && !report.extractedText.trim().isEmpty()) {
                        context.append("Extracted Text:\n").append(report.extractedText).append("\n");
                    }
                    if (report.aiInsights != null && !report.aiInsights.trim().isEmpty()) {
                        context.append("\nPrevious AI Analysis:\n").append(report.aiInsights);
                    }
                    String prompt = "You are a compassionate clinical companion for a diabetic patient. " +
                            "Based on the lab report below, explain the results in simple, easy-to-understand language. " +
                            "Highlight any high, low, or borderline values and give practical advice on what to discuss with the doctor.\n\n" +
                            context;
                    showTypingIndicator();
                    setInputEnabled(false);
                    chatService.chatWithAI(prompt, new OllamaRouter.TextCallback() {
                        @Override
                        public void onSuccess(String text) {
                            mainHandler.post(() -> {
                                if (!isAdded() || binding == null) return;
                                hideTypingIndicator();
                                setInputEnabled(true);
                                addMessage(text, false);
                            });
                        }

                        @Override
                        public void onFailure(String error) {
                            mainHandler.post(() -> {
                                if (!isAdded() || binding == null) return;
                                hideTypingIndicator();
                                setInputEnabled(true);
                                addMessage("Connection error: " + error, false);
                            });
                        }
                    });
                });
            });
        });

        // Medication reminders chip
        binding.chipMedication.setOnClickListener(v -> {
            addMessage("Medication reminders", true);
            hideTipCard();
            sendToAI("As my diabetic health assistant, give me a brief summary of best practices for managing my medication schedule for diabetes. Keep it practical and easy to follow.");
        });

        // Diet recommendations chip
        binding.chipDiet.setOnClickListener(v -> {
            addMessage("Diet recommendations", true);
            hideTipCard();
            sendToAI("As my diabetic health assistant, give me personalized diet and nutrition recommendations for a diabetic patient. Include foods to favor, foods to avoid, and meal timing tips.");
        });

        // Send button
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                addMessage(text, true);
                hideTipCard();
                sendToAI(text);
                binding.etMessage.setText("");
            }
        });

        // Keyboard send action
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String text = binding.etMessage.getText().toString().trim();
                if (!text.isEmpty()) {
                    addMessage(text, true);
                    hideTipCard();
                    sendToAI(text);
                    binding.etMessage.setText("");
                }
                return true;
            }
            return false;
        });
    }

    private void sendToAI(String userText) {
        showTypingIndicator();
        setInputEnabled(false);
        chatService.chatWithAI(userText, new OllamaRouter.TextCallback() {
            @Override
            public void onSuccess(String text) {
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    hideTypingIndicator();
                    setInputEnabled(true);
                    addMessage(text, false);
                });
            }

            @Override
            public void onFailure(String error) {
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    hideTypingIndicator();
                    setInputEnabled(true);
                    addMessage("Connection error: " + error, false);
                });
            }
        });
    }

    private void handleAttachment(Uri uri) {
        if (uri == null || binding == null) return;
        String name = getDisplayName(uri);
        addMessage("Attached: " + name, true);
        hideTipCard();
        showTypingIndicator();
        setInputEnabled(false);
        final boolean[] attachmentDone = {false};
        android.content.Context appContext = requireContext().getApplicationContext();
        mainHandler.postDelayed(() -> {
            if (!isAdded() || binding == null || attachmentDone[0]) return;
            attachmentDone[0] = true;
            hideTypingIndicator();
            setInputEnabled(true);
            addMessage("Attachment is taking longer than expected. I stopped waiting so the chat does not freeze. Try uploading the report from the Reports screen for full analysis.", false);
        }, 25000);

        OCRService.recognizeText(requireContext(), uri, new OCRService.OCRCallback() {
            @Override
            public void onSuccess(String extractedText) {
                if (attachmentDone[0]) return;
                attachmentDone[0] = true;
                dbExecutor.execute(() -> {
                    MedicalReport report = new MedicalReport();
                    report.title = name;
                    report.reportType = "Chat Attachment";
                    report.extractedText = extractedText;
                    report.aiInsights = "Saved from chat attachment. Ask the assistant to explain this report.";
                    report.imageURL = uri.toString();
                    AppDatabase.getInstance(appContext).appDao().insertReport(report);
                    mainHandler.post(() -> {
                        if (!isAdded() || binding == null) return;
                        hideTypingIndicator();
                        setInputEnabled(true);
                        addMessage("Attachment saved. Ask me to explain the attached report, or use the Reports screen for full lab extraction.", false);
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                if (attachmentDone[0]) return;
                attachmentDone[0] = true;
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    hideTypingIndicator();
                    setInputEnabled(true);
                    addMessage("I could not read that attachment: " + e.getMessage(), false);
                });
            }
        });
    }

    private void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your health assistant");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Voice input is not available on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getDisplayName(Uri uri) {
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        }
        return "selected file";
    }

    private void hideTipCard() {
        if (binding.dailyTipCard != null) {
            binding.dailyTipCard.setVisibility(View.GONE);
        }
    }

    private void addMessage(String text, boolean isUser) {
        if (!isAdded() || binding == null) return;
        messages.add(new ChatAdapter.Message(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvChat.scrollToPosition(messages.size() - 1);
        saveChatHistory();
    }

    private void loadChatHistory() {
        String saved = requireContext()
                .getSharedPreferences("chat_history", android.content.Context.MODE_PRIVATE)
                .getString("messages", "");
        if (saved == null || saved.trim().isEmpty()) return;
        String[] rows = saved.split("\\n");
        for (String row : rows) {
            if (row.startsWith("You: ")) {
                messages.add(new ChatAdapter.Message(row.substring(5), true));
            } else if (row.startsWith("Diabo: ")) {
                messages.add(new ChatAdapter.Message(row.substring(7), false));
            }
        }
    }

    private void saveChatHistory() {
        StringBuilder out = new StringBuilder();
        int start = Math.max(0, messages.size() - 60);
        for (int i = start; i < messages.size(); i++) {
            ChatAdapter.Message msg = messages.get(i);
            if ("\u2022 \u2022 \u2022".equals(msg.text)) continue;
            out.append(msg.isUser ? "You: " : "Diabo: ")
                    .append(msg.text.replace("\n", " "))
                    .append("\n");
        }
        requireContext()
                .getSharedPreferences("chat_history", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("messages", out.toString())
                .apply();
    }

    /** Show a "typing..." bubble as the last message */
    private void showTypingIndicator() {
        if (isWaitingForResponse) return;
        isWaitingForResponse = true;
        messages.add(new ChatAdapter.Message("\u2022 \u2022 \u2022", false));
        adapter.notifyItemInserted(messages.size() - 1);
        binding.rvChat.scrollToPosition(messages.size() - 1);
    }

    /** Remove the typing indicator bubble */
    private void hideTypingIndicator() {
        if (!isWaitingForResponse) return;
        isWaitingForResponse = false;
        if (!messages.isEmpty()) {
            int lastIdx = messages.size() - 1;
            ChatAdapter.Message last = messages.get(lastIdx);
            if (!last.isUser && "\u2022 \u2022 \u2022".equals(last.text)) {
                messages.remove(lastIdx);
                adapter.notifyItemRemoved(lastIdx);
                saveChatHistory();
            }
        }
    }

    /** Disable/enable input controls while waiting for AI */
    private void setInputEnabled(boolean enabled) {
        if (binding == null) return;
        binding.etMessage.setEnabled(enabled);
        binding.btnSend.setEnabled(enabled);
        binding.btnSend.setAlpha(enabled ? 1f : 0.4f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbExecutor.shutdown();
    }
}
