import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatWidgetComponent } from './components/chat-widget/chat-widget.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, ChatWidgetComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  mobileMenuOpen = false;

  // Profile metadata
  profilePhoto = 'assets/abhishek-mandloi.jpg';
  resumePdf = 'assets/Abhishek_Mandloi_Resume.pdf';
  phone = '6265494851';
  location = 'Indore, India';

  // Contact form model
  contact = {
    name: '',
    email: '',
    subject: '',
    message: ''
  };

  // Profile data structures
  skillsCategories = [
    {
      name: 'Programming & Big Data',
      icon: 'fa-solid fa-server',
      skills: ['Core Java', 'J2EE', 'Spring Boot', 'Apache Kafka', 'HDFS', 'YARN', 'Gathr Platform', 'Apache Spark']
    },
    {
      name: 'Database & NoSQL',
      icon: 'fa-solid fa-database',
      skills: ['HBase', 'MySQL (RDBMS)']
    },
    {
      name: 'Tools & Platforms',
      icon: 'fa-solid fa-toolbox',
      skills: ['Klera', 'Gathr', 'WinSCP', 'Offset Explorer']
    },
    {
      name: 'DevOps & Containers',
      icon: 'fa-solid fa-cubes',
      skills: ['Jenkins', 'Docker', 'Kubernetes']
    },
    {
      name: 'IDEs & Version Control',
      icon: 'fa-solid fa-code-branch',
      skills: ['IntelliJ IDEA', 'Windsurf', 'Git', 'SVN']
    },
    {
      name: 'AI Tools & Frameworks',
      icon: 'fa-solid fa-brain',
      skills: ['Google Gemini API', 'GitHub Copilot', 'AWS AI Services', 'RAG Architecture']
    }
  ];

  experiences = [
    {
      role: 'Senior Software Engineer',
      company: 'Impetus Technologies (India) Pvt. Ltd.',
      duration: 'March 2022 – Present',
      location: 'Indore, India',
      description: 'Specializing in scalable Big Data engineering, robust backend systems, and innovative AI-driven solutions.',
      projects: [
        {
          title: 'Project: Metatrail',
          techSummary: 'Java, J2EE, Spring Boot, Hadoop, HBase, Gathr, Klera, Kafka',
          metrics: '~650M records/day | 60% performance gain',
          tech: ['Core Java', 'Spring Boot', 'Apache Kafka', 'HBase', 'HDFS', 'YARN', 'Gathr', 'Klera'],
          bullets: [
            'Researched and delivered optimum solutions for a large-scale social media Big Data problem, focused on stream processing, transformation, and scalable storage on HDFS/YARN cluster environments.',
            'Engineered and released a production-grade data pipeline on the Gathr platform, processing ~650 million social media records per day for transformation and analysis.',
            'Owned end-to-end delivery of a Java-based business service for HBase data retrieval — from design through production release — improving data retrieval performance by 60%.',
            'Validated and deployed data pipelines and services into production through rigorous testing, ensuring reliable releases with minimal post-deployment issues.',
            'Diagnosed and resolved critical bugs and performance bottlenecks across pipelines and services, strengthening overall system reliability.'
          ]
        },
        {
          title: 'Project: RAG-based Call Center Chatbot',
          techSummary: 'AWS, RAG, Generative AI, Gemini, GitHub Copilot',
          metrics: '70% query resolution time reduction',
          tech: ['AWS AI Services', 'Google Gemini API', 'RAG Architecture', 'GitHub Copilot'],
          bullets: [
            'Designed and built a Retrieval-Augmented Generation (RAG) chatbot on AWS to automate FAQ handling for call center support, reducing average query resolution time by 70%.',
            'Leveraged Google Gemini and GitHub Copilot to accelerate development cycles and improve solution design and code quality.'
          ]
        }
      ]
    }
  ];

  educationList = [
    {
      degree: 'Bachelor of Technology (B.Tech) - Computer Science',
      institution: 'Sage University, Indore',
      period: '2018 – 2022',
      details: 'Specialized in computer science core principles, algorithms, distributed computing, database systems, and software engineering.'
    },
    {
      degree: '12th Standard (Higher Secondary Education)',
      institution: 'Jawahar Navodaya Vidyalaya, Indore',
      period: '2017 – 2018',
      details: 'Completed senior secondary education focusing on Physics, Chemistry, and Mathematics.'
    },
    {
      degree: '10th Standard (Secondary School Education)',
      institution: 'Jawahar Navodaya Vidyalaya, Indore',
      period: '2015 – 2016',
      details: 'Foundational secondary education with distinction academic record.'
    }
  ];

  awardsList = [
    {
      title: 'Star of the Month Award',
      organization: 'Impetus Technologies',
      description: 'Received multiple times for consistent high performance, dedication, and outstanding technical contribution to project goals.'
    },
    {
      title: 'Excellence Award (Transformational Performance)',
      organization: 'Impetus Technologies',
      description: 'Awarded in recognition of transformational impact, driving high data retrieval efficiency, and outstanding project delivery.'
    }
  ];

  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  sendEmail(event: Event) {
    event.preventDefault();
    const mailtoSubject = encodeURIComponent(this.contact.subject || 'Portfolio Inquiry');
    const mailtoBody = encodeURIComponent(`Name: ${this.contact.name}\nEmail: ${this.contact.email}\n\nMessage:\n${this.contact.message}`);
    window.location.href = `mailto:abhimandloi111@gmail.com?subject=${mailtoSubject}&body=${mailtoBody}`;
  }
}
