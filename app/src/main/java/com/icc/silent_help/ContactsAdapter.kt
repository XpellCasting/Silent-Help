package com.icc.silent_help
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(private val contacts: List<EmergencyContact>) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    // Este ViewHolder contiene las vistas para cada item de la lista.
    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val initials: TextView = itemView.findViewById(R.id.tv_initials)
        val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        val relationship: TextView = itemView.findViewById(R.id.tv_contact_relationship)
        val phone: TextView = itemView.findViewById(R.id.tv_contact_phone)
    }

    // Crea nuevos ViewHolders (invocado por el layout manager).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    // Reemplaza el contenido de una vista (invocado por el layout manager).
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.initials.text = contact.initials
        holder.name.text = contact.name
        holder.relationship.text = contact.relationship
        holder.phone.text = contact.phone
    }

    // Devuelve el tamaño de tu dataset (invocado por el layout manager).
    override fun getItemCount() = contacts.size
}