package cliente;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import com.google.gson.Gson;

import intefaces.Chat;
import intefaces.MenuInicio;
import intefaces.Notificacion;
import intefaces.Sala;
import intefaces.VentanaPrincipal;
import paqueteEnvios.Comando;
import paqueteEnvios.Paquete;
import paqueteEnvios.PaqueteDeSalas;
import paqueteEnvios.PaqueteDeUsuariosYSalas;
import paqueteEnvios.PaqueteMencion;
import paqueteEnvios.PaqueteMensaje;
import paqueteEnvios.PaqueteMensajeSala;
import paqueteEnvios.PaqueteSala;

public class EscuchaServer extends Thread {

	private Cliente cliente;
	private ObjectInputStream entrada;
	private final Gson gson = new Gson();
	private Chat chat;
	private Sala sala;

	protected static ArrayList<String> usuariosConectados = new ArrayList<String>();

	public EscuchaServer(final Cliente cliente) {
		this.cliente = cliente;
		this.entrada = cliente.getEntrada();
	}

	@Override
	public void run() {
		try {
			Paquete paquete;
			ArrayList<String> usuariosAntiguos = new ArrayList<String>();
			ArrayList<String> diferenciaContactos = new ArrayList<String>();

			String objetoLeido;

			objetoLeido = (String) entrada.readObject();
			while (true) {

				paquete = gson.fromJson(objetoLeido, Paquete.class);

				switch (paquete.getComando()) {

				// CONEXION = SE CONECTO OTRO USUARIO, ENTONCES LE MANDO LA
				// LISTA
				// A TODOS LOS USUARIOS ANTERIORES A EL

				case Comando.CONEXION:
					usuariosConectados = (ArrayList<String>) gson.fromJson(objetoLeido, PaqueteDeUsuariosYSalas.class)
					.getUsuarios();
					for (String usuario : usuariosConectados) {
						if (!usuariosAntiguos.contains(usuario)) {
							usuariosAntiguos.add(usuario);
						}
					}
					diferenciaContactos = new ArrayList<String>(usuariosAntiguos);
					diferenciaContactos.removeAll(usuariosConectados);
					if (!diferenciaContactos.isEmpty()) {
						for (String usuario : diferenciaContactos) {
							if (cliente.getChatsActivos().containsKey(usuario)) {
								cliente.getChatsActivos().get(usuario).getChat()
								.append("El usuario: " + usuario + " se ha desconectado\n");
							}
							usuariosAntiguos.remove(usuario);
						}
					}
					cliente.getPaqueteUsuario().setListaDeConectados(usuariosConectados);
					actualizarLista(cliente);
					break;

					// ACA RECIBI EL MENSAJE DEL OTRO CLIENTE
				case Comando.TALK:

					cliente.setPaqueteMensaje((PaqueteMensaje) gson.fromJson(objetoLeido, PaqueteMensaje.class));

					if (!(cliente.getChatsActivos().containsKey(cliente.getPaqueteMensaje().getUserEmisor()))) {
						chat = new Chat(cliente);

						chat.setTitle(cliente.getPaqueteMensaje().getUserEmisor());
						chat.setVisible(true);

						cliente.getChatsActivos().put(cliente.getPaqueteMensaje().getUserEmisor(), chat);
					}
					cliente.getChatsActivos().get(cliente.getPaqueteMensaje().getUserEmisor()).getChat()
					.append(cliente.getPaqueteMensaje().getUserEmisor() + ": "
							+ cliente.getPaqueteMensaje().getMsj() + "\n");
					cliente.getChatsActivos().get(cliente.getPaqueteMensaje().getUserEmisor()).getTexto().grabFocus();
					break;

				case Comando.MENCIONSALA:

					PaqueteMencion paqMenc = new PaqueteMencion();
					paqMenc = (PaqueteMencion) gson.fromJson(objetoLeido, PaqueteMencion.class);

					cliente.getPaqueteMencion().setMsj(paqMenc.getMsj());
					cliente.getPaqueteMencion().setUserEmisor(paqMenc.getUserEmisor());
					cliente.getPaqueteMencion().setUserReceptor(paqMenc.getUserReceptor());
					cliente.getPaqueteMencion().setNombreSala(paqMenc.getNombreSala());

					if((cliente.getSalasActivas().containsKey(cliente.getPaqueteMencion().getNombreSala()))){
						cliente.getSalasActivas().get(cliente.getPaqueteMencion().getNombreSala()).getChat()
						.append(cliente.getPaqueteMencion().getUserEmisor() + ": " + cliente.getPaqueteMencion().getMsj() + "\n");
						cliente.getSalasActivas().get(cliente.getPaqueteMencion().getNombreSala()).getTexto().grabFocus();
					}

					if ((cliente.getPaqueteUsuario().getUsername().equals(cliente.getPaqueteMencion().getUserReceptor()))) {
						Notificacion notificacion = new Notificacion(cliente.getPaqueteMencion().getNombreSala(),cliente.getPaqueteMencion().getUserEmisor());
						notificacion.displayTray();
					}
					break;

				case Comando.CHATSALA:

					PaqueteMensajeSala paq = new PaqueteMensajeSala();
					paq = (PaqueteMensajeSala) gson.fromJson(objetoLeido, PaqueteMensajeSala.class);

					cliente.getPaqueteMensajeSala().setMsj(paq.getMsj());
					cliente.getPaqueteMensajeSala().setNombreSala(paq.getNombreSala());
					cliente.getPaqueteMensajeSala().setUserEmisor(paq.getUserEmisor());


					if((cliente.getSalasActivas().containsKey(cliente.getPaqueteMensajeSala().getNombreSala()))){

						cliente.getSalasActivas().get(cliente.getPaqueteMensajeSala().getNombreSala()).getChat()
						.append(cliente.getPaqueteMensajeSala().getUserEmisor() + ": " + cliente.getPaqueteMensajeSala().getMsj() + "\n");
						cliente.getSalasActivas().get(cliente.getPaqueteMensajeSala().getNombreSala()).getTexto().grabFocus();

					}
					break;

				case Comando.CHATALL:

					cliente.setPaqueteMensaje((PaqueteMensaje) gson.fromJson(objetoLeido, PaqueteMensaje.class));
					if (!cliente.getChatsActivos().containsKey("Sala")) {

						VentanaPrincipal.setTextoChatGeneral(cliente.getPaqueteMensaje().getUserEmisor(),cliente.getPaqueteMensaje().getMsj());
					}
					break;

				case Comando.MP:

					cliente.setPaqueteMensaje((PaqueteMensaje) gson.fromJson(objetoLeido, PaqueteMensaje.class));

					if (!(cliente.getChatsActivos().containsKey(cliente.getPaqueteMensaje().getUserEmisor()))) {
						chat = new Chat(cliente);

						chat.setTitle(cliente.getPaqueteMensaje().getUserEmisor());
						chat.setVisible(true);

						cliente.getChatsActivos().put(cliente.getPaqueteMensaje().getUserEmisor(), chat);
					}
					cliente.getChatsActivos().get(cliente.getPaqueteMensaje().getUserEmisor()).getChat()
					.append(cliente.getPaqueteMensaje().getUserEmisor() + ": "
							+ cliente.getPaqueteMensaje().getMsj() + "\n");
					cliente.getChatsActivos().get(cliente.getPaqueteMensaje().getUserEmisor()).getTexto().grabFocus();
					break;

				case Comando.NEWSALA:
					cliente.getPaqueteUsuario().setMsj(paquete.getMsj());
					if( paquete.getMsj().equals(Paquete.msjExito)) {
						ArrayList<String> listadoSalas = (ArrayList<String>) gson.fromJson(objetoLeido, PaqueteDeSalas.class)
								.getSalas();
						cliente.getPaqueteUsuario().setListaDeSalas(listadoSalas);
						actualizarListaSalas(cliente);
					} else {
						JOptionPane.showMessageDialog(null, "Sala ya existente.");
					}
					break;
					
				case Comando.ELIMINARSALA:
					if(paquete.getMsj().equals(Paquete.msjExito)) {
						PaqueteSala paqueteSala = (PaqueteSala) gson.fromJson(objetoLeido, PaqueteSala.class);
						if(cliente.getSalasActivas().containsKey(paqueteSala.getNombreSala()) 
								|| cliente.getPaqueteUsuario().getUsername().equals(paqueteSala.getCliente())) {
							JOptionPane.showMessageDialog(null, "La sala " + paqueteSala.getNombreSala() + " ha sido eliminada.");
							if(cliente.getSalasActivas().containsKey(paqueteSala.getNombreSala()))
								cliente.getSalasActivas().get(paqueteSala.getNombreSala()).dispose();
							cliente.getSalasActivas().remove(paqueteSala.getNombreSala());
						}
						cliente.getPaqueteUsuario().getListaDeSalas().remove(paqueteSala.getNombreSala());
						actualizarListaSalas(cliente);
						break;
					} else if(paquete.getMsj().equals(Paquete.msjFracaso)) {
						JOptionPane.showMessageDialog(null, "Error al tratar de eliminar la sala.");
					} else if(paquete.getMsj().equals(Paquete.msjFallo)){
						JOptionPane.showMessageDialog(null, "La sala solo puede ser eliminada por el user que la creo ");
					}
					break;

				case Comando.ENTRARSALA:
					cliente.setPaqueteSala( gson.fromJson(objetoLeido, PaqueteSala.class));

					if (cliente.getPaqueteSala().getMsj().equals(Paquete.msjExito)) {
						if (!(cliente.getSalasActivas().containsKey(cliente.getPaqueteSala().getNombreSala()))) {
							sala = new Sala(cliente);
							sala.getChat().setText(cliente.getPaqueteSala().getHistorial());
							cliente.getSalasActivas().put(cliente.getPaqueteSala().getNombreSala(), sala);
						} else {
							JOptionPane.showMessageDialog(null, "Ya se encuentra conectado a esta sala");
						} 
					} else {
						JOptionPane.showMessageDialog(null, "Error al intentar entrar en la sala " + cliente.getPaqueteSala().getNombreSala());
					}
					break;

				case Comando.CONEXIONSALA:
					PaqueteSala paqueteSala = gson.fromJson(objetoLeido, PaqueteSala.class);
					if(cliente.getSalasActivas().containsKey(paqueteSala.getNombreSala())) {
						actualizarListaConectadosSala(paqueteSala);
					}
					
					break;
					
				case Comando.DESCONECTARDESALA:
					paqueteSala = gson.fromJson(objetoLeido, PaqueteSala.class);
					cliente.getSalasActivas().remove(paqueteSala.getNombreSala());
				}

				synchronized (entrada) {
					objetoLeido = (String) entrada.readObject();
				}
			}
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Fallo la conexión con el servidor.");
			e.printStackTrace();
		}
	}

	private void actualizarListaConectadosSala(PaqueteSala paqueteSala) {
		DefaultListModel<String> modelo = new DefaultListModel<String>();
		synchronized (cliente) {
			try {
				cliente.wait(5);
				cliente.getSalasActivas().get(paqueteSala.getNombreSala())
				.getListaConectadosSala().removeAll();

				if (paqueteSala.getUsuariosConectados() != null) {
					paqueteSala.getUsuariosConectados()
					.remove(cliente.getPaqueteUsuario().getUsername());
					for (String cad : paqueteSala.getUsuariosConectados()) {
						modelo.addElement(cad);
					}
					cliente.getSalasActivas().get(paqueteSala.getNombreSala()).getListaConectadosSala().setModel(modelo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void actualizarLista(final Cliente cliente) {
		DefaultListModel<String> modelo = new DefaultListModel<String>();
		synchronized (cliente) {
			try {
				cliente.wait(300);
				VentanaPrincipal.getListConectados().removeAll();
				if (cliente.getPaqueteUsuario().getListaDeConectados() != null) {
					cliente.getPaqueteUsuario().getListaDeConectados()
					.remove(cliente.getPaqueteUsuario().getUsername());
					for (String cad : cliente.getPaqueteUsuario().getListaDeConectados()) {
						modelo.addElement(cad);
					}
					VentanaPrincipal.setCantUsuariosCon(modelo.getSize());
					VentanaPrincipal.getListConectados().setModel(modelo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void actualizarListaSalas(final Cliente cliente) {
		DefaultListModel<String> modelo = new DefaultListModel<String>();
		synchronized (cliente) {
			try {
				cliente.wait(300);
				VentanaPrincipal.getListSalas().removeAll();
				if (cliente.getPaqueteUsuario().getListaDeSalas() != null) {
					for (String cad : cliente.getPaqueteUsuario().getListaDeSalas()) {
						modelo.addElement(cad);
					}
					VentanaPrincipal.getListSalas().setModel(modelo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static ArrayList<String> getUsuariosConectados() {
		return usuariosConectados;
	}
}