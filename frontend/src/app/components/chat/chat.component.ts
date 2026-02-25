import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { TextFieldModule } from '@angular/cdk/text-field';
import { ApiService, ChatMessage, ChatRequest } from '../../services/api.service';

interface DisplayMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  isLoading?: boolean;
  isError?: boolean;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    TextFieldModule
  ],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('chatContainer') private chatContainer!: ElementRef;
  @ViewChild('messageInput') private messageInput!: ElementRef;

  messages: DisplayMessage[] = [];
  userInput = '';
  isLoading = false;
  selectedModel = 'gemma-3-27b-it';
  availableModels: string[] = [];
  portfolioSummary: any = null;

  suggestedQueries = [
    'What is my total investment?',
    'Which company has the highest investment?',
    'Show me my top 5 holdings by value',
    'What is my average cost per share for each stock?',
    'How many different companies do I own?',
    'Which stocks did I buy most recently?',
    'What is the total quantity of shares I own?',
    'Summarize my portfolio performance'
  ];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadModels();
    this.loadContext();
    this.addWelcomeMessage();
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private loadModels(): void {
    this.apiService.getChatModels().subscribe({
      next: (models) => {
        this.availableModels = models;
        if (models.length > 0 && !models.includes(this.selectedModel)) {
          this.selectedModel = models[0];
        }
      },
      error: () => {
        this.availableModels = ['gemma-3-27b-it', 'pixtral-12b-2409'];
      }
    });
  }

  private loadContext(): void {
    this.apiService.getChatContext().subscribe({
      next: (context) => {
        this.portfolioSummary = context.portfolioSummary;
      },
      error: () => {}
    });
  }

  private addWelcomeMessage(): void {
    this.messages.push({
      role: 'assistant',
      content: `Hello! I'm your Portfolio Assistant. I have access to your investment data and can answer questions about your holdings, purchases, and portfolio performance.\n\nTry asking me something like:\n• "What is my total investment?"\n• "Which stocks do I own the most of?"\n• "Show me my recent purchases"`,
      timestamp: new Date()
    });
  }

  sendMessage(): void {
    const message = this.userInput.trim();
    if (!message || this.isLoading) return;

    this.messages.push({
      role: 'user',
      content: message,
      timestamp: new Date()
    });

    this.userInput = '';
    this.isLoading = true;

    const loadingMessage: DisplayMessage = {
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isLoading: true
    };
    this.messages.push(loadingMessage);

    const history: ChatMessage[] = this.messages
      .filter(m => !m.isLoading && !m.isError)
      .slice(-10)
      .map(m => ({ role: m.role, content: m.content }));

    const request: ChatRequest = {
      message,
      model: this.selectedModel,
      history: history.slice(0, -1)
    };

    this.apiService.sendChatMessage(request).subscribe({
      next: (response) => {
        this.messages = this.messages.filter(m => !m.isLoading);
        this.messages.push({
          role: 'assistant',
          content: response.message,
          timestamp: new Date()
        });
        this.isLoading = false;
      },
      error: (error) => {
        this.messages = this.messages.filter(m => !m.isLoading);
        this.messages.push({
          role: 'assistant',
          content: `Sorry, I encountered an error: ${error.error?.error || error.message || 'Unknown error'}. Please try again.`,
          timestamp: new Date(),
          isError: true
        });
        this.isLoading = false;
      }
    });
  }

  useSuggestion(query: string): void {
    this.userInput = query;
    this.sendMessage();
  }

  clearChat(): void {
    this.messages = [];
    this.addWelcomeMessage();
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.chatContainer) {
        this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
      }
    } catch (err) {}
  }

  formatMessage(content: string): string {
    return content
      .replace(/\n/g, '<br>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code>$1</code>');
  }
}
