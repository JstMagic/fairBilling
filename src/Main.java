import com.sun.deploy.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * TThis class process the a log file passed in from the command line argument
 * and it prints a minor report on each user session usage
 */
public class Main {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            int i = 0;
            File file;

            /**
             * initialize the userSession variable
             */
            List<Session> userSessions = new LinkedList<>();
            /**
             * get the file location from the argument
             */
            file = new File(args[0]);
            if (!file.isFile()) {
                System.out.println("Please provide the right directory for the log file");
                return;
            }
            try {
                /**
                 *  Creates a new <FileReader from given the File
                 */
                FileReader fileReader = new FileReader(file);
                /**
                 *  Creates a new BufferedReader from given the File
                 */
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                /**
                 * assign line value from each line read from the file
                 */
                while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
                    /**
                     * create a new session object and assign the values, these will be used at a later time
                     */
                    if (!StringUtils.trimWhitespace(line).isEmpty()) {
                        String[] sessions = StringUtils.splitString(line, " ");
                        Session session = new Main().new Session();
                        session.setTimeStamp(LocalTime.parse(sessions[0]));
                        session.setName(sessions[1]);
                        session.setAction(sessions[2]);
                        /**
                         * add the object to the userSession list
                         */
                        userSessions.add(session);
                    }
                    i++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            /**
             * LinkedList used so it keeps the order of the objects
             */
            List<Session> sessionAlreadySeen = new LinkedList<>();
//            Map<String, List<Session>> sessionAlreadySeen = new HashMap<>();
            /**
             * streem the object and get only the object name
             */
            userSessions.stream().map(Session::getName).collect(Collectors.toList()).forEach(name -> {
                Session userSession = null;

                List<Session> noNeed = new ArrayList<>();
                for (int idx = 0; idx < userSessions.size(); idx++) {
                    if (userSessions.get(idx).getName().equals(name) && !sessionAlreadySeen.contains(userSessions.get(idx))) {

                        userSession = userSessions.get(idx);

                        /**
                         * FineSessionByName will loop through the userSessions and returns either
                         * the user end session or the start of the session based on the Session type from the userSession model
                         */
                        Session foundUserSession = FindSessionByName(userSessions, userSession);
                        /**
                         * null check for foundUserSession
                         */
                        if (foundUserSession != null) {
                            /**
                             * trying to keep the order of the object returns and when adding it to the list
                             */

                            userSession.addSession();
                            if (userSession.getAction().equalsIgnoreCase("start")) {
                                userSession.addTimeinMin(Long.valueOf(userSession.getTimeStamp().until(foundUserSession.getTimeStamp(), ChronoUnit.SECONDS)).intValue());
                                foundUserSession.addTimeinMin(Long.valueOf(userSession.getTimeStamp().until(foundUserSession.getTimeStamp(), ChronoUnit.SECONDS)).intValue());
                                sessionAlreadySeen.add(userSession);
                                sessionAlreadySeen.add(foundUserSession);
                                foundUserSession.addSession();
                                userSession.addSession();
                            }
                            if (userSession.getAction().equalsIgnoreCase("end")) {
                                userSession.addTimeinMin(Long.valueOf(foundUserSession.getTimeStamp().until(userSession.getTimeStamp(), ChronoUnit.SECONDS)).intValue());
                                foundUserSession.addTimeinMin(Long.valueOf(foundUserSession.getTimeStamp().until(userSession.getTimeStamp(), ChronoUnit.SECONDS)).intValue());
                                sessionAlreadySeen.add(foundUserSession);
                                sessionAlreadySeen.add(userSession);
                                userSession.addSession();
                                foundUserSession.addSession();
                            }


                        } else {
                            sessionAlreadySeen.add(userSession);
                        }

                    }
                }
            });

            for(Map.Entry<String, List<Session>> sessionMap : sessionAlreadySeen.stream().collect(Collectors.groupingBy(x -> x.getName())).entrySet()){
                Collections.sort(sessionMap.getValue(), Comparator.comparing(a->a.getSession(), Comparator.reverseOrder()));

                Session session = sessionMap.getValue().get(0);
                System.out.println(sessionMap.getKey() +"   "+session.getSession()+"    "+session.getTotalTime() );
            }

        } else {
            System.out.println("Please ensure you have provided the location for the log file");
        }


    }

    /**
     * @param userSessions
     * @param userSessh
     * @return
     */
    private static Session FindSessionByName(List<Session> userSessions, Session userSessh) {
        Session session = null;

        if (userSessh.getAction().equalsIgnoreCase("start"))  /* get user end session */ {
            int indx = userSessions.indexOf(userSessh);
            /**
             * loop forward since we are only interested in the user end time session
             */
            for (; indx < userSessions.size(); indx++) {
                Session usersSession = userSessions.get(indx);
                if ((usersSession.getTimeStamp().isAfter(userSessh.getTimeStamp()) && usersSession.getAction().equalsIgnoreCase("end") && userSessh.getAction().equalsIgnoreCase("start"))) {
                    session = usersSession;
                    if (indx < userSessions.size() && userSessions.get(indx + 1).getName().equals(userSessh.getName())) {
                        session = userSessions.get(indx + 1);
                        break;
                    }

                }
            }
        } else if (userSessh.getAction().equalsIgnoreCase("end")) /* get user start session */ {
            /**
             * loop backwards since we are only interested in the user start session
             */
            int indx = userSessions.indexOf(userSessh);
            for (; indx >= 0; indx--) {
                Session usersSession = userSessions.get(indx);
                /**
                 * if user is found based on certain condition assign the session and break
                 */
                if (usersSession.getTimeStamp().isBefore(userSessh.getTimeStamp()) && usersSession.getAction().equalsIgnoreCase("start") && userSessh.getAction().equalsIgnoreCase("end")) {
                    session = usersSession;
                    break;
                }
            }

        }
        /**
         * finally return the session
         */
        return session;
    }

    /**
     * The type Session.
     */
    class Session {

        /**
         * The Name.
         */
        String name;
        /**
         * The Time stamp.
         */
        LocalTime timeStamp = null;
        /**
         * The Session.
         */
        int session;
        /**
         * The Total time.
         */
        int totalTime;
        /**
         * The Action.
         */
        String action;

        /**
         * Gets name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets name.
         *
         * @param name the name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets time stamp.
         *
         * @return the time stamp
         */
        public LocalTime getTimeStamp() {
            return timeStamp;
        }

        /**
         * Sets time stamp.
         *
         * @param timeStamp the time stamp
         */
        public void setTimeStamp(LocalTime timeStamp) {
            this.timeStamp = timeStamp;
        }

        /**
         * Gets session.
         *
         * @return the session
         */
        public int getSession() {
            return session;
        }

        /**
         * Sets session.
         *
         * @param session the session
         */
        public void setSession(int session) {
            this.session = session;
        }

        /**
         * Gets total time.
         *
         * @return the total time
         */
        public int getTotalTime() {
            return totalTime;
        }

        /**
         * Sets total time.
         *
         * @param totalTime the total time
         */
        public void setTotalTime(int totalTime) {
            this.totalTime = totalTime;
        }

        /**
         * Gets action.
         *
         * @return the action
         */
        public String getAction() {
            return action;
        }

        /**
         * Sets action.
         *
         * @param action the action
         */
        public void setAction(String action) {
            this.action = action;
        }

        /**
         * Add timein min.
         *
         * @param min the min
         */
        public void addTimeinMin(int min) {
            totalTime += min;
        }

        /**
         * Add session.
         */
        public void addSession() {
            session++;
        }

        @Override
        public String toString() {
            return "Session{" +
                    "name='" + name + '\'' +
                    ", timeStamp=" + timeStamp +
                    ", session=" + session +
                    ", totalTime=" + totalTime +
                    ", action='" + action + '\'' +
                    '}' + "\n";
        }
    }
}
