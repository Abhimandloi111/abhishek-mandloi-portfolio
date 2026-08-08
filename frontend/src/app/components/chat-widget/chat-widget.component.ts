import { Component, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessage } from '../../services/chat.service';

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss']
})
export class ChatWidgetComponent implements AfterViewChecked {
  @ViewChild('scrollContainer') private scrollContainer!: ElementRef;

  isOpen = false;
  isLoading = false;
  userInput = '';

  messages: ChatMessage[] = [
    {
      sender: 'bot',
      text: "Hello! I'm Abhishek's AI Assistant powered by a Java Spring Boot RAG pipeline & Google Gemini API. Ask me anything about his experience, Big Data projects, skills, or education!",
      timestamp: new Date(),
      sources: ['RAG Context Index'],
      poweredByGemini: true
    }
  ];

  suggestedQuestions = [
    "Tell me about the Metatrail project",
    "What AI & RAG solutions have you built?",
    "What Big Data tools do you use?",
    "Summarize your experience at Impetus"
  ];

  constructor(private chatService: ChatService) {}

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.scrollToBottom(), 100);
    }
  }

  sendSuggested(question: string) {
    this.userInput = question;
    this.sendMessage();
  }

  sendMessage() {
    const question = this.userInput.trim();
    if (!question || this.isLoading) return;

    // Add user message
    this.messages.push({
      sender: 'user',
      text: question,
      timestamp: new Date()
    });

    this.userInput = '';
    this.isLoading = true;

    this.chatService.askQuestion(question).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.messages.push({
          sender: 'bot',
          text: res.answer,
          timestamp: new Date(),
          sources: res.sources,
          poweredByGemini: res.poweredByGemini
        });
      },
      error: () => {
        this.isLoading = false;
        this.messages.push({
          sender: 'bot',
          text: "Sorry, I ran into an error processing your query. Please try again or reach out to Abhishek directly via email!",
          timestamp: new Date()
        });
      }
    });
  }

  clearChat() {
    this.messages = [
      {
        sender: 'bot',
        text: "Chat history cleared. How can I help you regarding Abhishek Mandloi's profile?",
        timestamp: new Date(),
        sources: ['System Reset']
      }
    ];
  }

  private scrollToBottom(): void {
    try {
      if (this.scrollContainer) {
        this.scrollContainer.nativeElement.scrollTop = this.scrollContainer.nativeElement.scrollHeight;
      }
    } catch(err) { }
  }
}
