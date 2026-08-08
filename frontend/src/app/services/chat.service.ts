import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

export interface ChatMessage {
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
  sources?: string[];
  poweredByGemini?: boolean;
}

export interface ChatResponse {
  answer: string;
  sources: string[];
  poweredByGemini: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  // Update this to your deployed Spring Boot backend URL in production
  private apiUrl = 'http://localhost:8080/api/chat';

  constructor(private http: HttpClient) {}

  askQuestion(question: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.apiUrl, { question }).pipe(
      catchError(error => {
        console.error('API call failed, providing client fallback:', error);
        return of({
          answer: "I'm having trouble connecting to Abhishek's backend service right now. You can reach out directly via email at abhimandloi111@gmail.com or on LinkedIn!",
          sources: ["System: Offline"],
          poweredByGemini: false
        });
      })
    );
  }
}
