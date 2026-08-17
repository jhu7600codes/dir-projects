export type Json = string | number | boolean | null | { [key: string]: Json | undefined } | Json[];

export type Database = {
  __InternalSupabase: {
    PostgrestVersion: "14.15";
  };
  public: {
    Tables: {
      devices: {
        Row: {
          created_at: string;
          id: string;
          last_seen_at: string;
          name: string;
          pair_code: string;
          platform: string;
          push_subscription: Json | null;
        };
        Insert: {
          created_at?: string;
          id?: string;
          last_seen_at?: string;
          name: string;
          pair_code: string;
          platform: string;
          push_subscription?: Json | null;
        };
        Update: {
          created_at?: string;
          id?: string;
          last_seen_at?: string;
          name?: string;
          pair_code?: string;
          platform?: string;
          push_subscription?: Json | null;
        };
        Relationships: [];
      };
      transfers: {
        Row: {
          created_at: string;
          expires_at: string;
          file_paths: Json;
          id: string;
          responded_at: string | null;
          sender_device_id: string;
          status: Database["public"]["Enums"]["transfer_status"];
          target_device_id: string;
        };
        Insert: {
          created_at?: string;
          expires_at?: string;
          file_paths?: Json;
          id?: string;
          responded_at?: string | null;
          sender_device_id: string;
          status?: Database["public"]["Enums"]["transfer_status"];
          target_device_id: string;
        };
        Update: {
          created_at?: string;
          expires_at?: string;
          file_paths?: Json;
          id?: string;
          responded_at?: string | null;
          sender_device_id?: string;
          status?: Database["public"]["Enums"]["transfer_status"];
          target_device_id?: string;
        };
        Relationships: [
          {
            foreignKeyName: "transfers_sender_device_id_fkey";
            columns: ["sender_device_id"];
            isOneToOne: false;
            referencedRelation: "devices";
            referencedColumns: ["id"];
          },
          {
            foreignKeyName: "transfers_target_device_id_fkey";
            columns: ["target_device_id"];
            isOneToOne: false;
            referencedRelation: "devices";
            referencedColumns: ["id"];
          },
        ];
      };
    };
    Views: {
      [_ in never]: never;
    };
    Functions: {
      [_ in never]: never;
    };
    Enums: {
      transfer_status: "pending" | "accepted" | "declined" | "expired";
    };
    CompositeTypes: {
      [_ in never]: never;
    };
  };
};
